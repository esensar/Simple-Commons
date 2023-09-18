package com.simplemobiletools.commons.dialogs

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.compose.extensions.config
import com.simplemobiletools.commons.databinding.DialogRadioGroupBinding
import com.simplemobiletools.commons.databinding.RadioButtonBinding
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.StorageLocation

/**
 * A dialog for choosing between internal, root, SD card (optional) storage
 *
 * @param activity has to be activity to avoid some Theme.AppCompat issues
 * @param currPath current path to decide which storage should be preselected
 * @param pickSingleOption if only one option like "Internal" is available, select it automatically
 * @param callback an anonymous function
 *
 */
class StoragePickerDialog(
    val activity: BaseSimpleActivity,
    val currPath: String,
    val showRoot: Boolean,
    pickSingleOption: Boolean,
    private val allowAddingCustomLocations: Boolean = false,
    val callback: (pickedPath: String) -> Unit
) {
    private val ID_INTERNAL = 1
    private val ID_SD = 2
    private val ID_OTG = 3
    private val ID_ROOT = 4
    private val ID_CUSTOM_START = 5

    private lateinit var radioGroup: RadioGroup
    private var dialog: AlertDialog? = null
    private var defaultSelectedId = 0
    private val availableStorages = ArrayList<String>()

    init {
        availableStorages.add(activity.internalStoragePath)
        val customLocations = activity.config.customStorageLocations
        when {
            activity.hasExternalSDCard() -> availableStorages.add(activity.sdCardPath)
            activity.hasOTGConnected() -> availableStorages.add("otg")
            showRoot -> availableStorages.add("root")
        }

        if (pickSingleOption && availableStorages.size == 1 && customLocations.isEmpty()) {
            callback(availableStorages.first())
        } else {
            initDialog()
        }
    }

    private fun initDialog() {
        val inflater = LayoutInflater.from(activity)
        val resources = activity.resources
        val layoutParams = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val view = DialogRadioGroupBinding.inflate(inflater, null, false)
        radioGroup = view.dialogRadioGroup
        val basePath = currPath.getBasePath(activity)

        val internalButton = RadioButtonBinding.inflate(inflater, null, false).root
        internalButton.apply {
            id = ID_INTERNAL
            text = resources.getString(R.string.internal)
            isChecked = basePath == context.internalStoragePath
            setOnClickListener { internalPicked() }
            if (isChecked) {
                defaultSelectedId = id
            }
        }
        radioGroup.addView(internalButton, layoutParams)

        if (activity.hasExternalSDCard()) {
            val sdButton = RadioButtonBinding.inflate(inflater, null, false).root
            sdButton.apply {
                id = ID_SD
                text = resources.getString(R.string.sd_card)
                isChecked = basePath == context.sdCardPath
                setOnClickListener { sdPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(sdButton, layoutParams)
        }

        if (activity.hasOTGConnected()) {
            val otgButton = RadioButtonBinding.inflate(inflater, null, false).root
            otgButton.apply {
                id = ID_OTG
                text = resources.getString(R.string.usb)
                isChecked = basePath == context.otgPath
                setOnClickListener { otgPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(otgButton, layoutParams)
        }

        // allow for example excluding the root folder at the gallery
        if (showRoot) {
            val rootButton = RadioButtonBinding.inflate(inflater, null, false).root
            rootButton.apply {
                id = ID_ROOT
                text = resources.getString(R.string.root)
                isChecked = basePath == "/"
                setOnClickListener { rootPicked() }
                if (isChecked) {
                    defaultSelectedId = id
                }
            }
            radioGroup.addView(rootButton, layoutParams)
        }

        var customId = ID_CUSTOM_START
        val customLocations = activity.config.customStorageLocations

        customLocations.forEach { storage ->
            val customButton = RadioButtonBinding.inflate(inflater, null, false).root
            customButton.apply {
                id = customId++
                text = storage.name
                isChecked = basePath == storage.treeUri
                setOnClickListener { customPicked(storage.treeUri) }
            }
            radioGroup.addView(customButton, layoutParams)
        }

        if (allowAddingCustomLocations) {
            val customButton = RadioButtonBinding.inflate(inflater, null, false).root
            customButton.apply {
                id = customId++
                text = resources.getString(R.string.custom)
                isChecked = false
                setOnClickListener { newCustomPicked() }
            }
            radioGroup.addView(customButton, layoutParams)
        }

        activity.getAlertDialogBuilder().apply {
            activity.setupDialogStuff(view.root, this, R.string.select_storage) { alertDialog ->
                dialog = alertDialog
            }
        }
    }

    private fun internalPicked() {
        dialog?.dismiss()
        callback(activity.internalStoragePath)
    }

    private fun sdPicked() {
        dialog?.dismiss()
        callback(activity.sdCardPath)
    }

    private fun otgPicked() {
        activity.handleOTGPermission {
            if (it) {
                callback(activity.otgPath)
                dialog?.dismiss()
            } else {
                radioGroup.check(defaultSelectedId)
            }
        }
    }

    private fun rootPicked() {
        dialog?.dismiss()
        callback("/")
    }

    private fun customPicked(uri: String) {
        dialog?.dismiss()
        callback(uri)
    }

    private fun newCustomPicked() {
        activity.handleNewDocumentTreeDialog { success, uri ->
            if (success && !activity.isPathOnCustomStorageLocation(uri.toString())) {
                dialog?.dismiss()

                val customLocations = activity.config.customStorageLocations.toMutableList()
                val documentFile = DocumentFile.fromTreeUri(activity, uri!!)
                customLocations.add(StorageLocation(uri.toString(), documentFile?.name ?: ""))
                activity.config.customStorageLocations = customLocations

                initDialog()
            }
        }
    }
}
