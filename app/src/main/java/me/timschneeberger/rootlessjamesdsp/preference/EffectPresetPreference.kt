package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.preset.BuiltInPresets
import me.timschneeberger.rootlessjamesdsp.model.preset.EffectPresetManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast

/**
 * A Preference that shows a preset picker dialog when clicked.
 * Displays built-in presets and user-saved presets for the associated effect.
 * The effect namespace is set via the custom XML attribute `app:effectNamespace`.
 */
class EffectPresetPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    var effectNamespace: String = ""

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.EffectPresetPreference)) {
            effectNamespace = getString(R.styleable.EffectPresetPreference_effectNamespace) ?: ""
            recycle()
        }
        isPersistent = false
    }

    override fun onClick() {
        showPresetDialog()
    }

    private fun showPresetDialog() {
        val builtInPresets = BuiltInPresets.getPresetsForEffect(effectNamespace)
        val userPresetNames = EffectPresetManager.listPresets(context, effectNamespace)

        // Build the list of items
        val items = mutableListOf<String>()
        val isBuiltIn = mutableListOf<Boolean>()

        if (builtInPresets.isNotEmpty()) {
            items.add("── ${context.getString(R.string.preset_section_builtin)} ──")
            isBuiltIn.add(false) // header, not selectable via direct map
            for (preset in builtInPresets) {
                items.add("  ${preset.name}")
                isBuiltIn.add(true)
            }
        }

        if (userPresetNames.isNotEmpty()) {
            items.add("── ${context.getString(R.string.preset_section_user)} ──")
            isBuiltIn.add(false) // header
            for (name in userPresetNames) {
                items.add("  $name")
                isBuiltIn.add(false)
            }
        }

        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.preset_select_title)
            .setNeutralButton(R.string.preset_save_current) { _, _ ->
                showSavePresetDialog()
            }
            .setNegativeButton(android.R.string.cancel, null)

        if (items.isEmpty()) {
            dialogBuilder.setMessage(R.string.preset_none_available)
        } else {
            dialogBuilder.setItems(items.toTypedArray()) { _, which ->
                val selectedText = items[which].trim()
                // Skip headers
                if (selectedText.startsWith("──")) return@setItems

                // Check if this is a built-in preset
                var builtInIndex = -1
                if (builtInPresets.isNotEmpty()) {
                    var counter = 0
                    for (i in builtInPresets.indices) {
                        // Item index: 1 (after header) + i
                        if (which == 1 + i) {
                            builtInIndex = i
                            break
                        }
                    }
                }

                if (builtInIndex >= 0) {
                    val preset = builtInPresets[builtInIndex]
                    BuiltInPresets.applyEffectPreset(context, effectNamespace, preset)
                    context.sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
                    context.sendLocalBroadcast(Intent(Constants.ACTION_PRESET_LOADED))
                    context.toast(context.getString(R.string.preset_applied, preset.name), false)
                } else {
                    // User preset — calculate index
                    val userSectionStart = if (builtInPresets.isNotEmpty())
                        1 + builtInPresets.size + 1 // header + presets + user header
                    else
                        1 // just user header

                    val userIndex = which - userSectionStart
                    if (userIndex in userPresetNames.indices) {
                        val presetName = userPresetNames[userIndex]
                        showUserPresetActionDialog(presetName)
                    }
                }
            }
        }

        dialogBuilder.show()
    }

    private fun showUserPresetActionDialog(presetName: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle(presetName)
            .setItems(arrayOf(
                context.getString(R.string.preset_action_load),
                context.getString(R.string.preset_action_delete)
            )) { _, which ->
                when (which) {
                    0 -> {
                        EffectPresetManager.loadPreset(context, effectNamespace, presetName)
                        context.sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
                        context.sendLocalBroadcast(Intent(Constants.ACTION_PRESET_LOADED))
                        context.toast(context.getString(R.string.preset_applied, presetName), false)
                    }
                    1 -> {
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.preset_delete_confirm_title)
                            .setMessage(context.getString(R.string.preset_delete_confirm_message, presetName))
                            .setPositiveButton(R.string.preset_action_delete) { _, _ ->
                                EffectPresetManager.deletePreset(context, effectNamespace, presetName)
                                context.toast(context.getString(R.string.preset_deleted, presetName), false)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSavePresetDialog() {
        val inputView = LayoutInflater.from(context).inflate(R.layout.dialog_textinput, null)
        val editText = inputView.findViewById<EditText>(android.R.id.text1)
        editText.hint = context.getString(R.string.preset_name_hint)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.preset_save_title)
            .setView(inputView)
            .setPositiveButton(R.string.preset_save_action) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (EffectPresetManager.savePreset(context, effectNamespace, name)) {
                        context.toast(context.getString(R.string.preset_saved, name), false)
                    } else {
                        context.toast(context.getString(R.string.preset_save_error), false)
                    }
                } else {
                    context.toast(context.getString(R.string.preset_name_empty), false)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
