/*
 * Copyright (C) 2015-2017 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegravity.rteditor;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.onegravity.rteditor.utils.Helper;
import com.onegravity.rteditor.utils.validator.EmailValidator;
import com.onegravity.rteditor.utils.validator.UrlValidator;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

/**
 * A DialogFragment to add, modify or remove links from Spanned text.
 */
public class LinkFragment extends DialogFragment {

    private static final String LINK_ADDRESS = "link_address";
    private static final String LINK_TEXT = "link_text";

    /**
     * The Link class describes a link (link text and an URL).
     */
    static class Link {
        final private String mLinkText;
        final private String mUrl;

        private Link(String linkText, String url) {
            mLinkText = linkText;
            mUrl = url;
        }

        public String getLinkText() {
            return mLinkText;
        }

        public String getUrl() {
            return mUrl;
        }

        public boolean isValid() {
            return mUrl != null && mUrl.length() > 0 && mLinkText != null && mLinkText.length() > 0;
        }
    }

    /**
     * This event is broadcast via EventBus when the dialog closes.
     * It's received by the RTManager to update the active editor.
     */
    static class LinkEvent {
        private final String mFragmentTag;
        private final Link mLink;
        private final boolean mWasCancelled;

        public LinkEvent(Fragment fragment, Link link, boolean wasCancelled) {
            mFragmentTag = fragment.getTag();
            mLink = link;
            mWasCancelled = wasCancelled;
        }

        public String getFragmentTag() {
            return mFragmentTag;
        }

        public Link getLink() {
            return mLink;
        }

        public boolean wasCancelled() {
            return mWasCancelled;
        }
    }

    private static final UrlValidator sUrlValidator = new UrlValidator(UrlValidator.ALLOW_2_SLASHES + UrlValidator.ALLOW_ALL_SCHEMES);
    private static final EmailValidator sEmailValidator = EmailValidator.getInstance(false);

    public static LinkFragment newInstance(String linkText, String url) {
        LinkFragment fragment = new LinkFragment();
        Bundle args = new Bundle();
        args.putString(LINK_TEXT, linkText);
        args.putString(LINK_ADDRESS, url);
        fragment.setArguments(args);
        return fragment;
    }

    public LinkFragment() {
    }

    @SuppressLint("InflateParams")
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.rte_link, null);

        Bundle args = getArguments();

        // link address
        String tmp = "http://";
        final String address = args.getString(LINK_ADDRESS);
        if (address != null && ! address.isEmpty()) {
            try {
                Uri uri = Uri.parse( Helper.decodeQuery(address) );
                // if we have an email address remove the mailto: part for editing purposes
                tmp = startsWithMailto(address) ? uri.getSchemeSpecificPart() : uri.toString();
            } catch (Exception ignore) {}
        }
        final String url = tmp;
        final TextView addressView = ((TextView) view.findViewById(R.id.linkURL));
        if (url != null) {
            addressView.setText(url);
        }

        // link text
        String linkText = args.getString(LINK_TEXT);
        final TextView textView = ((TextView) view.findViewById(R.id.linkText));
        if (linkText != null) {
            textView.setText(linkText);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.rte_create_a_link)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // OK button
                        validate(dialog, addressView, textView);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel button
                        EventBus.getDefault().post(new LinkEvent(LinkFragment.this, new Link(null, url), true));
                    }
                });

        if (address != null) {
            builder.setNeutralButton(R.string.rte_remove_action, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Remove button
                    EventBus.getDefault().post(new LinkEvent(LinkFragment.this, null, false));
                }
            });
        }

        return builder.create();
    }

    private void validate(DialogInterface dialog, TextView addressView, TextView textView) {
        // retrieve link address and do some cleanup
        final String address = addressView.getText().toString().trim();

        boolean isEmail = sEmailValidator.isValid(address);
        boolean isUrl = sUrlValidator.isValid(address);
        if (requiredFieldValid(addressView) && (isUrl || isEmail)) {
            // valid url or email address

            // encode address
            String newAddress = Helper.encodeUrl(address);

            // add mailto: for email addresses
            if (isEmail && !startsWithMailto(newAddress)) {
                newAddress = "mailto:" + newAddress;
            }

            // use the original address text as link text if the user didn't enter anything
            String linkText = textView.getText().toString();
            if (linkText.length() == 0) {
                linkText = address;
            }

            EventBus.getDefault().post(new LinkEvent(LinkFragment.this, new Link(linkText, newAddress), false));
            try { dialog.dismiss(); } catch (Exception ignore) {}
        } else {
            // invalid address (neither a url nor an email address
            String errorMessage = getString(R.string.rte_invalid_link, address);
            addressView.setError(errorMessage);
        }
    }

    private boolean startsWithMailto(String address) {
        return address != null && address.toLowerCase(Locale.getDefault()).startsWith("mailto:");
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        EventBus.getDefault().post(new LinkEvent(LinkFragment.this, null, true));
    }

    private boolean requiredFieldValid(TextView view) {
        return view.getText() != null && view.getText().length() > 0;
    }
}