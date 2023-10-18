package com.simplemobiletools.commons.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.compose.alert_dialog.AlertDialogState
import com.simplemobiletools.commons.compose.alert_dialog.DialogSurface
import com.simplemobiletools.commons.compose.alert_dialog.rememberAlertDialogState
import com.simplemobiletools.commons.compose.extensions.MyDevices
import com.simplemobiletools.commons.compose.theme.AppThemeSurface
import com.simplemobiletools.commons.compose.theme.SimpleTheme
import com.simplemobiletools.commons.databinding.DialogSelectAlarmSoundBinding
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SILENT
import com.simplemobiletools.commons.models.AlarmSound
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyCompatRadioButton

class SelectAlarmSoundDialog(
    val activity: BaseSimpleActivity, val currentUri: String, val audioStream: Int, val pickAudioIntentId: Int,
    val type: Int, val loopAudio: Boolean, val onAlarmPicked: (alarmSound: AlarmSound?) -> Unit,
    val onAlarmSoundDeleted: (alarmSound: AlarmSound) -> Unit
) {
    private val ADD_NEW_SOUND_ID = -2

    private val view = DialogSelectAlarmSoundBinding.inflate(activity.layoutInflater,  null, false)
    private var systemAlarmSounds = ArrayList<AlarmSound>()
    private var yourAlarmSounds = ArrayList<AlarmSound>()
    private var mediaPlayer: MediaPlayer? = null
    private val config = activity.baseConfig
    private var dialog: AlertDialog? = null

    init {
        activity.getAlarmSounds(type) {
            systemAlarmSounds = it
            gotSystemAlarms()
        }

        view.dialogSelectAlarmYourLabel.setTextColor(activity.getProperPrimaryColor())
        view.dialogSelectAlarmSystemLabel.setTextColor(activity.getProperPrimaryColor())

        addYourAlarms()

        activity.getAlertDialogBuilder()
            .setOnDismissListener { mediaPlayer?.stop() }
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view.root, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.window?.volumeControlStream = audioStream
                }
            }
    }

    private fun addYourAlarms() {
        view.dialogSelectAlarmYourRadio.removeAllViews()
        val token = object : TypeToken<ArrayList<AlarmSound>>() {}.type
        yourAlarmSounds = Gson().fromJson<ArrayList<AlarmSound>>(config.yourAlarmSounds, token) ?: ArrayList()
        yourAlarmSounds.add(AlarmSound(ADD_NEW_SOUND_ID, activity.getString(R.string.add_new_sound), ""))
        yourAlarmSounds.forEach {
            addAlarmSound(it, view.dialogSelectAlarmYourRadio)
        }
    }

    private fun gotSystemAlarms() {
        systemAlarmSounds.forEach {
            addAlarmSound(it, view.dialogSelectAlarmSystemRadio)
        }
    }

    private fun addAlarmSound(alarmSound: AlarmSound, holder: ViewGroup) {
        val radioButton = (activity.layoutInflater.inflate(R.layout.item_select_alarm_sound, null) as MyCompatRadioButton).apply {
            text = alarmSound.title
            isChecked = alarmSound.uri == currentUri
            id = alarmSound.id
            setColors(activity.getProperTextColor(), activity.getProperPrimaryColor(), activity.getProperBackgroundColor())
            setOnClickListener {
                alarmClicked(alarmSound)

                if (holder == view.dialogSelectAlarmSystemRadio) {
                    view.dialogSelectAlarmYourRadio.clearCheck()
                } else {
                    view.dialogSelectAlarmSystemRadio.clearCheck()
                }
            }

            if (alarmSound.id != -2 && holder == view.dialogSelectAlarmYourRadio) {
                setOnLongClickListener {
                    val items = arrayListOf(RadioItem(1, context.getString(R.string.remove)))

                    RadioGroupDialog(activity, items) {
                        removeAlarmSound(alarmSound)
                    }
                    true
                }
            }
        }

        holder.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun alarmClicked(alarmSound: AlarmSound) {
        when {
            alarmSound.uri == SILENT -> mediaPlayer?.stop()
            alarmSound.id == ADD_NEW_SOUND_ID -> {
                val action = Intent.ACTION_OPEN_DOCUMENT
                val intent = Intent(action).apply {
                    type = "audio/*"
                    flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                }

                try {
                    activity.startActivityForResult(intent, pickAudioIntentId)
                } catch (e: ActivityNotFoundException) {
                    activity.toast(R.string.no_app_found)
                }
                dialog?.dismiss()
            }
            else -> try {
                mediaPlayer?.reset()
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioStreamType(audioStream)
                        isLooping = loopAudio
                    }
                }

                mediaPlayer?.apply {
                    setDataSource(activity, Uri.parse(alarmSound.uri))
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }

    private fun removeAlarmSound(alarmSound: AlarmSound) {
        val token = object : TypeToken<ArrayList<AlarmSound>>() {}.type
        yourAlarmSounds = Gson().fromJson<ArrayList<AlarmSound>>(config.yourAlarmSounds, token) ?: ArrayList()
        yourAlarmSounds.remove(alarmSound)
        config.yourAlarmSounds = Gson().toJson(yourAlarmSounds)
        addYourAlarms()

        if (alarmSound.id == view.dialogSelectAlarmYourRadio.checkedRadioButtonId) {
            view.dialogSelectAlarmYourRadio.clearCheck()
            view.dialogSelectAlarmSystemRadio.check(systemAlarmSounds.firstOrNull()?.id ?: 0)
        }

        onAlarmSoundDeleted(alarmSound)
    }

    private fun dialogConfirmed() {
        if (view.dialogSelectAlarmYourRadio.checkedRadioButtonId != -1) {
            val checkedId = view.dialogSelectAlarmYourRadio.checkedRadioButtonId
            onAlarmPicked(yourAlarmSounds.firstOrNull { it.id == checkedId })
        } else {
            val checkedId = view.dialogSelectAlarmSystemRadio.checkedRadioButtonId
            onAlarmPicked(systemAlarmSounds.firstOrNull { it.id == checkedId })
        }
    }
}

@Composable
fun SelectAlarmSoundAlertDialog(
    alertDialogState: AlertDialogState,
    preselectedAlarm: Int,
    yourAlarms: List<AlarmSound>,
    systemAlarms: List<AlarmSound>,
    onAlarmChecked: (alarmSound: AlarmSound) -> Unit,
    onAlarmPicked: (alarmSound: AlarmSound?) -> Unit,
    onAlarmSoundDeleteRequested: (alarmSound: AlarmSound) -> Unit
) {
    var checkedAlarm by remember { mutableIntStateOf(preselectedAlarm) }

    AlertDialog(
        onDismissRequest = alertDialogState::hide,
    ) {
        DialogSurface {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.7f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = SimpleTheme.dimens.padding.extraLarge + SimpleTheme.dimens.padding.small)
                            .padding(top = SimpleTheme.dimens.padding.extraLarge + SimpleTheme.dimens.padding.small),
                        text = stringResource(id = R.string.your_sounds),
                        color = SimpleTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(SimpleTheme.dimens.padding.medium))
                    yourAlarms.forEach {
                        key(it.id) {
                            AlarmSoundItem(item = it, selected = checkedAlarm == it.id, onLongClick = { onAlarmSoundDeleteRequested(it) }) {
                                checkedAlarm = it.id
                                onAlarmChecked(it)
                            }
                        }
                    }
                    Text(
                        modifier = Modifier
                            .padding(horizontal = SimpleTheme.dimens.padding.extraLarge + SimpleTheme.dimens.padding.small)
                            .padding(top = SimpleTheme.dimens.padding.extraLarge + SimpleTheme.dimens.padding.small),
                        text = stringResource(id = R.string.system_sounds),
                        color = SimpleTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(SimpleTheme.dimens.padding.medium))
                    systemAlarms.forEach {
                        key(it.id) {
                            AlarmSoundItem(item = it, selected = checkedAlarm == it.id) {
                                checkedAlarm = it.id
                                onAlarmChecked(it)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        alertDialogState.hide()
                    }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }

                    TextButton(onClick = {
                        alertDialogState.hide()
                    }) {
                        Text(text = stringResource(id = R.string.ok))
                        onAlarmPicked(
                            (yourAlarms + systemAlarms).firstOrNull { it.id == checkedAlarm }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmSoundItem(
    item: AlarmSound,
    selected: Boolean,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = SimpleTheme.dimens.padding.extraLarge, vertical = SimpleTheme.dimens.padding.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(modifier = Modifier.padding(start = SimpleTheme.dimens.padding.medium), text = item.title)
    }
}

@Composable
@MyDevices
fun SelectAlarmSoundAlertDialogPreview() {
    AppThemeSurface {
        SelectAlarmSoundAlertDialog(
            alertDialogState = rememberAlertDialogState(),
            yourAlarms = listOf(),
            systemAlarms = listOf(),
            preselectedAlarm = -1,
            onAlarmChecked = {},
            onAlarmPicked = {},
            onAlarmSoundDeleteRequested = {}
        )
    }
}
