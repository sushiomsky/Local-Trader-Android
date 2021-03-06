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

package com.thanksmister.bitcoin.localtrader.ui.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thanksmister.bitcoin.localtrader.R;
import com.thanksmister.bitcoin.localtrader.data.database.DbManager;
import com.thanksmister.bitcoin.localtrader.data.database.ExchangeRateItem;
import com.thanksmister.bitcoin.localtrader.data.database.WalletItem;
import com.thanksmister.bitcoin.localtrader.events.ConfirmationDialogEvent;
import com.thanksmister.bitcoin.localtrader.events.ProgressDialogEvent;
import com.thanksmister.bitcoin.localtrader.network.api.model.WalletData;
import com.thanksmister.bitcoin.localtrader.network.services.DataService;
import com.thanksmister.bitcoin.localtrader.network.services.ExchangeService;
import com.thanksmister.bitcoin.localtrader.ui.BaseActivity;
import com.thanksmister.bitcoin.localtrader.ui.BaseFragment;
import com.thanksmister.bitcoin.localtrader.ui.activities.MainActivity;
import com.thanksmister.bitcoin.localtrader.ui.activities.PinCodeActivity;
import com.thanksmister.bitcoin.localtrader.utils.Calculations;
import com.thanksmister.bitcoin.localtrader.utils.Conversions;
import com.thanksmister.bitcoin.localtrader.utils.Doubles;
import com.thanksmister.bitcoin.localtrader.utils.WalletUtils;
import com.trello.rxlifecycle.FragmentEvent;

import java.lang.reflect.Field;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dpreference.DPreference;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class SendFragment extends BaseFragment {

    public static final String EXTRA_ADDRESS = "com.thanksmister.extra.EXTRA_ADDRESS";
    public static final String EXTRA_AMOUNT = "com.thanksmister.extra.EXTRA_AMOUNT";

    @Inject
    DataService dataService;

    @Inject
    DPreference dPreference;

    @Inject
    ExchangeService exchangeService;

    @Inject
    DbManager dbManager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.amountText)
    TextView amountText;

    @BindView(R.id.fiatEditText)
    TextView fiatEditText;

    @BindView(R.id.balanceText)
    TextView balance;

    @BindView(R.id.address)
    TextView addressText;

    @BindView(R.id.currencyText)
    TextView currencyText;

    @BindView(R.id.sendDescription)
    TextView sendDescription;

    @OnClick(R.id.sendButton)
    public void sendButtonClicked() {
        validateForm();
    }

    private String address;
    private String amount;
    private WalletData walletData;

    private Subscription sendSubscription;

    public static SendFragment newInstance(String address, String amount) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ADDRESS, address);
        args.putString(EXTRA_AMOUNT, amount);
        fragment.setArguments(args);
        return fragment;
    }

    public static SendFragment newInstance() {
        return new SendFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            address = getArguments().getString(EXTRA_ADDRESS);
            amount = getArguments().getString(EXTRA_AMOUNT);
        }

        // TODO make these static
        amount = dPreference.getString("send_amount", amount);
        address = dPreference.getString("send_address", address);

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        dPreference.putString("send_amount", amount);
        dPreference.putString("send_address", address);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        Timber.d("onActivityResult: requestCode " + requestCode);
        Timber.d("onActivityResult: resultCode " + resultCode);

        if (requestCode == PinCodeActivity.REQUEST_CODE) {
            if (resultCode == PinCodeActivity.RESULT_VERIFIED) {
                String pinCode = intent.getStringExtra(PinCodeActivity.EXTRA_PIN_CODE);
                String address = intent.getStringExtra(PinCodeActivity.EXTRA_ADDRESS);
                String amount = intent.getStringExtra(PinCodeActivity.EXTRA_AMOUNT);
                pinCodeEvent(pinCode, address, amount);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.send, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_paste:
                setAddressFromClipboard();
                return true;
            case R.id.action_scan:
                ((BaseActivity) getActivity()).launchScanner();
                return true;
            default:
                break;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_send, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        Timber.d("onViewCreated");

        if (!TextUtils.isEmpty(amount)) {
            amountText.setText(amount);
        }

        if (!TextUtils.isEmpty(address)) {
            addressText.setText(address);
        }

        sendDescription.setText(Html.fromHtml(getString(R.string.pin_code_send)));
        sendDescription.setMovementMethod(LinkMovementMethod.getInstance());

        addressText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (TextUtils.isEmpty(addressText.getText().toString())) {
                    setAddressFromClipboardTouch();
                }
                return false;
            }
        });

        amountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                amount = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (amountText.hasFocus()) {
                    String bitcoin = editable.toString();
                    calculateCurrencyAmount(bitcoin);
                }
            }
        });

        fiatEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (fiatEditText.hasFocus()) {
                    String amount = editable.toString();
                    calculateBitcoinAmount(amount);
                }
            }
        });

        setCurrency();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupToolbar();
        Timber.d("onActivityCreated");
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeData();
        setCurrency();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sendSubscription != null)
            sendSubscription.unsubscribe();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Timber.d("onDetach");

        dPreference.removePreference("send_amount");
        dPreference.removePreference("send_address");

        //http://stackoverflow.com/questions/15207305/getting-the-error-java-lang-illegalstateexception-activity-has-been-destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupToolbar() {
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        final ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
            ab.setTitle(getString(R.string.view_title_send));
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setCurrency() {
        String currency = exchangeService.getExchangeCurrency();

        if (currencyText != null)
            currencyText.setText(currency);
    }

    protected void subscribeData() {
        // this must be set each time
        CompositeSubscription dataSubscriptions = new CompositeSubscription();

        dataSubscriptions.add(Observable.combineLatest(dbManager.exchangeQuery(), dbManager.walletQuery(), new Func2<ExchangeRateItem, WalletItem, WalletData>() {
            @Override
            public WalletData call(ExchangeRateItem rateItem, WalletItem wallet) {
                WalletData walletData = null;
                if (wallet != null) {
                    walletData = new WalletData();
                    walletData.setAddress(wallet.address());
                    walletData.setBalance(wallet.sendable()); // only have sendable balance available to send
                    walletData.setRate("0");
                    if (rateItem != null) {
                        walletData.setRate(rateItem.rate());
                    }
                }
                return walletData;
            }
        })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("Wallet and exchange subscription safely unsubscribed");
                    }
                })
                .compose(this.<WalletData>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<WalletData>() {
                    @Override
                    public void call(WalletData results) {
                        walletData = results;
                        setWallet();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        reportError(throwable);
                    }
                }));
    }

    private void promptForPin(String bitcoinAddress, String bitcoinAmount) {
        Intent intent = PinCodeActivity.createStartIntent(getActivity(), bitcoinAddress, bitcoinAmount);
        startActivityForResult(intent, PinCodeActivity.REQUEST_CODE); // be sure to do this from fragment context
    }

    public void setAddressFromClipboardTouch() {

        String clipText = getClipboardText();
        if (TextUtils.isEmpty(clipText)) {
            return;
        }

        Timber.d("setAddressFromClipboardTouch");

        final String bitcoinAddress = WalletUtils.parseBitcoinAddress(clipText);
        validateBitcoinAddress(bitcoinAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<Boolean>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        Timber.d("onCompleted");
                    }

                    @Override
                    public void onError(final Throwable e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Timber.e(e.getMessage() + " for validating bitcoinAddress :" + bitcoinAddress);
                            }
                        });
                    }

                    @Override
                    public void onNext(final Boolean valid) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (valid) {
                                    Timber.d("valid: " + String.valueOf(true));
                                    setAddressFromClipboard();
                                }
                            }
                        });
                    }
                });
    }

    public void setAddressFromClipboard() {

        String clipText = getClipboardText();
        if (TextUtils.isEmpty(clipText)) {
            toast(R.string.toast_clipboard_empty);
            return;
        }

        Timber.d("setAddressFromClipboard");

        final String bitcoinAddress = WalletUtils.parseBitcoinAddress(clipText);
        final String bitcoinAmount = WalletUtils.parseBitcoinAmount(clipText);

        validateBitcoinAddress(bitcoinAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<Boolean>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(final Throwable e) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Timber.e(e.getMessage() + " for validating bitcoinAddress :" + bitcoinAddress);
                            }
                        });
                    }

                    @Override
                    public void onNext(final Boolean valid) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (valid) {
                                    Timber.d("valid: " + String.valueOf(true));
                                    setBitcoinAddress(bitcoinAddress);
                                    if (!TextUtils.isEmpty(bitcoinAmount)) {
                                        if (WalletUtils.validAmount(bitcoinAmount)) {
                                            setAmount(bitcoinAmount);
                                        } else {
                                            toast(getString(R.string.toast_invalid_btc_amount));
                                        }
                                    }
                                } else {
                                    toast(getString(R.string.toast_invalid_address));
                                }
                            }
                        });
                    }
                });
    }

    private Observable<Boolean> validateBitcoinAddress(final String address) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    String bitcoinAddress = WalletUtils.parseBitcoinAddress(address);
                    boolean valid = WalletUtils.validBitcoinAddress(bitcoinAddress);
                    Timber.d("validBitcoinAddress: " + valid);
                    subscriber.onNext(valid);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private String getClipboardText() {
        String clipText = "";
        ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboardManager.getPrimaryClip();
        if (clip != null) {
            ClipData.Item item = clip.getItemAt(0);
            if (item.getText() != null)
                clipText = item.getText().toString();
        }
        return clipText;
    }

    public void pinCodeEvent(final String pinCode, final String address, final String amount) {

        Timber.d("pinCodeEvent");

        this.address = address;
        this.amount = amount;

        String confirmTitle = "Confirm Transaction";
        String confirmDescription = getString(R.string.send_confirmation_description, amount, address);
        showConfirmationDialog(new ConfirmationDialogEvent(confirmTitle, confirmDescription, getString(R.string.button_confirm), getString(R.string.button_cancel), new Action0() {
            @Override
            public void call() {
                confirmedPinCodeSend(pinCode, address, amount);
            }
        }));
    }

    public void confirmedPinCodeSend(String pinCode, String address, String amount) {

        if (sendSubscription != null)
            return;

        showProgressDialog(new ProgressDialogEvent(getString(R.string.progress_sending_transaction)));

        sendSubscription = dataService.sendPinCodeMoney(pinCode, address, amount)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        hideProgressDialog();
                        resetWallet();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        hideProgressDialog();
                        handleSendError(throwable);
                    }
                });
    }

    private void handleSendError(Throwable throwable) {
        if (sendSubscription != null) {
            sendSubscription.unsubscribe();
            sendSubscription = null;
        }
        amountText.setText("");
        addressText.setText("");
        calculateCurrencyAmount("0.00");
        handleError(throwable);
    }

    public void setBitcoinAddress(String bitcoinAddress) {
        if (!TextUtils.isEmpty(bitcoinAddress)) {
            address = bitcoinAddress;
            addressText.setText(bitcoinAddress);
        }
    }

    public void setAmount(String bitcoinAmount) {
        if (!TextUtils.isEmpty(bitcoinAmount)) {
            amount = bitcoinAmount;
            amountText.setText(bitcoinAmount);
            calculateCurrencyAmount(bitcoinAmount);
        }
    }

    public void resetWallet() {
        if (isAdded()) {
            amount = "";
            address = "";
            amountText.setText("");
            addressText.setText("");
            calculateCurrencyAmount("0.00");
            toast(R.string.toast_transaction_success);
            ((MainActivity) getActivity()).navigateDashboardViewAndRefresh();
        }
    }

    public void setWallet() {
        computeBalance(0);
        setCurrency(); // update currency if there were any changes
        if (TextUtils.isEmpty(amountText.getText().toString())) {
            calculateCurrencyAmount("0.00");
        } else {
            calculateCurrencyAmount(amountText.getText().toString());
        }
    }

    // TODO validate that the balance is not negative
    protected void validateForm() {

        if (TextUtils.isEmpty(amountText.getText().toString())) {
            toast(getString(R.string.error_missing_address_amount));
            return;
        }

        amount = Conversions.formatBitcoinAmount(amountText.getText().toString());
        address = addressText.getText().toString();

        if (TextUtils.isEmpty(address)) {
            toast(getString(R.string.error_missing_address_amount));
            return;
        }

        if (TextUtils.isEmpty(amount) || !WalletUtils.validAmount(amount)) {
            toast(getString(R.string.toast_invalid_btc_amount));
            return;
        }

        validateBitcoinAddress(address)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Timber.i("validateBitcoinAddress subscription safely unsubscribed");
                    }
                })
                .compose(this.<Boolean>bindUntilEvent(FragmentEvent.PAUSE))
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(final Boolean valid) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (valid) {
                                    promptForPin(address, amount);
                                } else {
                                    toast(getString(R.string.toast_invalid_address));
                                }
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.w(throwable.getMessage());
                    }
                });
    }

    protected void computeBalance(double btcAmount) {
        Timber.d("Compute balance for: " + btcAmount);

        if (walletData == null) return;

        double balanceAmount = Conversions.convertToDouble(walletData.getBalance());
        String btcBalance = Conversions.formatBitcoinAmount(balanceAmount - btcAmount);

        String value = Calculations.computedValueOfBitcoin(walletData.getRate(), walletData.getBalance());
        String currency = exchangeService.getExchangeCurrency();
        if (balanceAmount < btcAmount) {
            balance.setText(Html.fromHtml(getString(R.string.form_balance_negative, btcBalance, value, currency)));
        } else {
            balance.setText(Html.fromHtml(getString(R.string.form_balance_positive, btcBalance, value, currency)));
        }
    }

    private void calculateBitcoinAmount(String fiat) {
        if (walletData == null) return;

        try {
            if (Doubles.convertToDouble(fiat) == 0) {
                computeBalance(0);
                amount = "";
                amountText.setText(amount);
                return;
            }
        } catch (Exception e) {
            reportError(e);
            return;
        }

        try {
            String exchangeValue = walletData.getRate();
            double btc = Math.abs(Doubles.convertToDouble(fiat) / Doubles.convertToDouble(exchangeValue));
            amount = Conversions.formatBitcoinAmount(btc);
            amountText.setText(amount); // set bitcoin amount
            computeBalance(btc);
        } catch (Exception e) {
            reportError(e);
        }
    }

    private void calculateCurrencyAmount(String bitcoin) {
        if (walletData == null) return;

        try {
            if (Doubles.convertToDouble(bitcoin) == 0) {
                fiatEditText.setText("");
                computeBalance(0);
                return;
            }
        } catch (Exception e) {
            reportError(e);
            return;
        }

        try {
            computeBalance(Doubles.convertToDouble(bitcoin));
            String value = Calculations.computedValueOfBitcoin(walletData.getRate(), bitcoin);
            fiatEditText.setText(value);
        } catch (Exception e) {
            reportError(e);
        }
    }
}