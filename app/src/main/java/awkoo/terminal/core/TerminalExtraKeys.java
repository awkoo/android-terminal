package awkoo.terminal.core;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONException;

import awkoo.terminal.Constants;
import awkoo.terminal.app.MainActivity;
import awkoo.terminal.view.extrakeys.ExtraKeysConstants;
import awkoo.terminal.view.extrakeys.ExtraKeysInfo;
import awkoo.terminal.utils.UI;
import awkoo.terminal.view.TerminalView;

public class TerminalExtraKeys extends awkoo.terminal.view.extrakeys.TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final MainActivity mActivity;
    final TerminalViewClient mTerminalViewClient;
    final TerminalSessionActivityClient mTerminalSessionActivityClient;

    public TerminalExtraKeys(MainActivity activity, @NonNull TerminalView terminalView,
                             TerminalViewClient terminalViewClient,
                             TerminalSessionActivityClient terminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mTerminalViewClient = terminalViewClient;
        mTerminalSessionActivityClient = terminalSessionActivityClient;

        setExtraKeys();
    }


    /**
     * Set the terminal extra keys and style.
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        try {
            // The mMap stores the extra key and style string values while loading properties
            // Check {@link #getExtraKeysInternalPropertyValueFromValue(String)} and
            // {@link #getExtraKeysStyleInternalPropertyValueFromValue(String)}
            String extrakeys = Constants.DEFAULT_IVALUE_EXTRA_KEYS;
            String extraKeysStyle = Constants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;

            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            UI.showToast(mActivity, "Can't create default extra keys", true);
            mExtraKeysInfo = null;
        }
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        switch (key) {
            case "KEYBOARD" -> {
                if (mTerminalViewClient != null)
                    mTerminalViewClient.onToggleSoftKeyboardRequest();
            }
            case "DRAWER" -> {
                DrawerLayout drawerLayout = mTerminalViewClient.getActivity().getDrawer();
                if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                    drawerLayout.closeDrawer(Gravity.LEFT);
                else
                    drawerLayout.openDrawer(Gravity.LEFT);
            }
            case "PASTE" -> {
                if (mTerminalSessionActivityClient != null)
                    mTerminalSessionActivityClient.onPasteTextFromClipboard();
            }
            case "SCROLL" -> {
                TerminalView terminalView = mTerminalViewClient.getActivity().getTerminalView();
                if (terminalView != null && terminalView.mEmulator != null)
                    terminalView.mEmulator.toggleAutoScrollDisabled();
            }
            case null, default ->
                super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
