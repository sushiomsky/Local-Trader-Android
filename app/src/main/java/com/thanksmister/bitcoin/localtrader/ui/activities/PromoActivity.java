/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.constants.Constants;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.utils.AuthUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class PromoActivity extends BaseActivity {

    @OnClick(R.id.registerButton)
    public void registerButtonClicked() {
        showRegistration();
    }

    @OnClick(R.id.loginButton)
    public void loginButtonClicked() {
        showLoginView();
    }


    public static Intent createStartIntent(Context context) {
        return new Intent(context, PromoActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_promo);
        ButterKnife.bind(this);
    }

    public void showLoginView() {
        Intent intent = LoginActivity.createStartIntent(PromoActivity.this);
        startActivity(intent);
    }

    public void showRegistration() {
        String url = Constants.REGISTRATION_URL;
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (SecurityException e) {
            showAlertDialogLinks(getString(R.string.error_traffic_rerouted));
        } catch (ActivityNotFoundException e) {
            showAlertDialogLinks(getString(R.string.toast_error_no_installed_ativity));
        }
    }
}