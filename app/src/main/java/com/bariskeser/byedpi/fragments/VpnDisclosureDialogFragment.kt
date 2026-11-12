package com.bariskeser.byedpi.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.utility.setVpnConsent

/**
 * In-app VPN disclosure. Shown before the first VPN connection (consent mode) and
 * from the settings screen (review mode).
 *
 * Consent mode: "Agree" stores the consent. "Decline" and Back send a result
 * without saving anything, so the user can be asked again later.
 *
 * Review mode: "Agree" stores the consent. "Decline" withdraws it.
 *
 * The outcome is delivered with [REQUEST_KEY] to the fragment manager that
 * showed the dialog, so the result survives configuration changes.
 */
class VpnDisclosureDialogFragment : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "vpn_consent"
        const val RESULT_ACCEPTED = "accepted"

        private const val ARG_REVIEW = "review"

        fun newInstance(review: Boolean): VpnDisclosureDialogFragment =
            VpnDisclosureDialogFragment().apply {
                arguments = bundleOf(ARG_REVIEW to review)
            }
    }

    private var answered = false

    private val reviewMode: Boolean
        get() = requireArguments().getBoolean(ARG_REVIEW, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val content = LayoutInflater.from(context).inflate(R.layout.dialog_vpn_disclosure, null)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.vpn_disclosure_title)
            .setView(content)
            .setPositiveButton(R.string.vpn_disclosure_agree) { _, _ -> answer(accepted = true) }
            .setNegativeButton(R.string.vpn_disclosure_decline) { _, _ -> answer(accepted = false) }
            .create()

        // Tapping outside does not count as an answer. Back is handled by onCancel.
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        answer(accepted = false)
    }

    private fun answer(accepted: Boolean) {
        if (answered) return
        answered = true

        val context = requireContext()
        if (accepted) {
            context.setVpnConsent(true)
        } else if (reviewMode) {
            context.setVpnConsent(false)
        }

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_ACCEPTED to accepted),
        )
    }
}
