package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.FACEBOOK;
import static com.prasunpersonal.jiscedebuggers.App.GOOGLE;
import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.prasunpersonal.jiscedebuggers.Adapters.FragmentAdapter;
import com.prasunpersonal.jiscedebuggers.Fragments.LoginFragment;
import com.prasunpersonal.jiscedebuggers.Fragments.SignupFragment;
import com.prasunpersonal.jiscedebuggers.Models.User;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.Services.FirebaseMessagingSender;
import com.prasunpersonal.jiscedebuggers.databinding.ActivitySignupLoginBinding;
import com.prasunpersonal.jiscedebuggers.databinding.PhoneAuthDialogLayoutBinding;
import com.prasunpersonal.jiscedebuggers.databinding.SignupLayoutBinding;

public class SignupLogin extends AppCompatActivity {
    private static final String TAG = SignupLogin.class.getSimpleName();
    ActivitySignupLoginBinding binding;
    GoogleSignInClient googleSignInClient;
    CallbackManager facebookCallbackManager;
    PhoneAuthProvider.ForceResendingToken otpResendToken;
    String phoneVerificationId;
    boolean codeSendOnce;
    CountDownTimer timer;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new SignupFragment());
        fragments.add(new LoginFragment());

        binding.signupLoginPager.setAdapter(new FragmentAdapter(getSupportFragmentManager(), getLifecycle(), fragments));
        binding.signupLoginPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.signupLoginTab.selectTab(binding.signupLoginTab.getTabAt(position));
                super.onPageSelected(position);
            }
        });
        binding.signupLoginTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.signupLoginPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build());
        facebookCallbackManager = CallbackManager.Factory.create();

        binding.googleLogin.setOnClickListener(v1 -> googleSignIn());
        binding.facebookLogin.setOnClickListener(v1 -> facebookLogIn());
        binding.phoneLogin.setOnClickListener(v1 -> phoneOtpLogin());

        binding.gitLogin.setOnClickListener(v1 -> Toast.makeText(this, "This feature will be available soon.", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void googleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleLoginActivity.launch(signInIntent);
    }

    private void facebookLogIn() {
        LoginManager manager = LoginManager.getInstance();
        manager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        manager.registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                loadFacebookAccount(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(SignupLogin.this, "Login canceled!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Toast.makeText(SignupLogin.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Facebook Login Error: ", e);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void phoneOtpLogin() {
        PhoneAuthDialogLayoutBinding phDialog = PhoneAuthDialogLayoutBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(phDialog.getRoot()).setCancelable(false);

        AlertDialog ad = builder.create();
        ad.getWindow().setBackgroundDrawable(ActivityCompat.getDrawable(getApplicationContext(), R.drawable.dialog_bg));
        ad.show();

        codeSendOnce = false;
        PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                phoneVerificationId = s;
                otpResendToken = forceResendingToken;
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                final String code = phoneAuthCredential.getSmsCode();
                if (code != null) {
                    phDialog.otp.setText(code);
                    phDialog.otp.setEnabled(false);
                    phDialog.verify.setEnabled(false);
                    phDialog.progressTitle.setText("Verifying OTP...");
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(phoneVerificationId, code);
                    loadPhoneAuthAccount(credential, phDialog, ad);
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(SignupLogin.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                codeSendOnce = false;
                phDialog.sPhone.setEnabled(true);
                phDialog.ccp.setCcpClickable(true);
                phDialog.sendOtp.setEnabled(true);
                phDialog.sendOtp.setText("Send OTP");
                phDialog.verificationArea.setVisibility(View.GONE);
                phDialog.progressArea.setVisibility(View.GONE);
                timer.cancel();
                Log.d(TAG, "onVerificationFailed: ", e);
            }
        };

        phDialog.sPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (codeSendOnce) {
                    codeSendOnce = false;
                    phDialog.sendOtp.setEnabled(true);
                    phDialog.sendOtp.setText("Send OTP");
                    phDialog.verificationArea.setVisibility(View.GONE);
                    phDialog.progressArea.setVisibility(View.GONE);
                }
                phDialog.sendOtp.setEnabled(s.toString().trim().length() == 10 && !s.toString().trim().contains(" "));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        phDialog.otp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phDialog.verify.setEnabled(s.toString().trim().length() == 6 && !s.toString().trim().contains(" "));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        phDialog.sendOtp.setOnClickListener(v -> {
            phDialog.sPhone.setEnabled(false);
            phDialog.ccp.setCcpClickable(false);
            phDialog.sendOtp.setEnabled(false);

            PhoneAuthOptions options;
            if (codeSendOnce) {
                options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phDialog.ccp.getSelectedCountryCodeWithPlus() + phDialog.sPhone.getText().toString().trim())
                        .setTimeout(120L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .setForceResendingToken(otpResendToken)
                        .build();
            } else {
                options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phDialog.ccp.getSelectedCountryCodeWithPlus() + phDialog.sPhone.getText().toString().trim())
                        .setTimeout(120L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
                codeSendOnce = true;
            }
            PhoneAuthProvider.verifyPhoneNumber(options);

            phDialog.verificationArea.setVisibility(View.VISIBLE);
            phDialog.progressArea.setVisibility(View.VISIBLE);
            phDialog.progressTitle.setText("Sending OTP...");

            timer = new CountDownTimer(120000, 1000){
                @Override
                public void onTick(long millisUntilFinished) {
                    phDialog.sendOtp.setText(String.format(Locale.getDefault(),"Resend OTP %d", (millisUntilFinished/1000)));
                }
                @Override
                public void onFinish() {
                    phDialog.sendOtp.setText("Resend OTP");
                    phDialog.sendOtp.setEnabled(true);
                    phDialog.sPhone.setEnabled(true);
                    phDialog.ccp.setCcpClickable(true);
                }
            }.start();
        });
        phDialog.verify.setOnClickListener(v -> {
            phDialog.otp.setEnabled(false);
            phDialog.verify.setEnabled(false);
            phDialog.progressTitle.setText("Verifying OTP...");
            phDialog.progressArea.setVisibility(View.VISIBLE);

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(phoneVerificationId, phDialog.otp.getText().toString());
            loadPhoneAuthAccount(credential, phDialog, ad);
        });

        phDialog.cancel.setOnClickListener(v -> ad.dismiss());
    }

    ActivityResultLauncher<Intent> googleLoginActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                loadGoogleAccount(task.getResult(ApiException.class));
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    });

    private void loadGoogleAccount(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                db.collection("Users").document(Objects.requireNonNull(auth.getUid())).get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        if (task1.getResult().exists()) {
                            ME = task1.getResult().toObject(User.class);
                            FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                            FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            User user = new User(Objects.requireNonNull(auth.getCurrentUser()).getDisplayName(), Objects.requireNonNull(account.getEmail()));
                            user.setProfilePic(String.valueOf(auth.getCurrentUser().getPhotoUrl()));
                            HashMap<String, String> authProvider = new HashMap<>();
                            authProvider.put(GOOGLE, account.getId());
                            user.setSocialAuthProviders(authProvider);
                            showRegisterDialog(user);
                        }
                    } else {
                        Toast.makeText(this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "loadFacebookAccount: ", task1.getException());
                    }
                });
            } else {
                Log.d(TAG, "firebaseAuthWithGoogle: ", task.getException());
            }
        });
    }

    private void loadFacebookAccount(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                db.collection("Users").document(Objects.requireNonNull(auth.getUid())).get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        if (task1.getResult().exists()) {
                            ME = task1.getResult().toObject(User.class);
                            FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                            FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            Bundle bundle = new Bundle();
                            bundle.putString("fields", "id,picture,name,email");
                            GraphRequest request = GraphRequest.newMeRequest(token, (object, response) -> {
                                if (object != null) {
                                    try {
                                        User user = new User(Objects.requireNonNull(auth.getCurrentUser()).getDisplayName(), Objects.requireNonNull(object.getString("email")));
                                        user.setProfilePic(String.valueOf(auth.getCurrentUser().getPhotoUrl()));
                                        HashMap<String, String> authProvider = new HashMap<>();
                                        authProvider.put(FACEBOOK, object.getString("id"));
                                        user.setSocialAuthProviders(authProvider);
                                        showRegisterDialog(user);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            request.setParameters(bundle);
                            request.executeAsync();
                        }
                    } else {
                        Toast.makeText(this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "loadFacebookAccount: ", task1.getException());
                    }
                });
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.getException());
                Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPhoneAuthAccount(PhoneAuthCredential credential, PhoneAuthDialogLayoutBinding phDialog, Dialog dialog) {
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                dialog.dismiss();
                db.collection("Users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).get().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()){
                        if (task1.getResult().exists()) {
                            ME = task1.getResult().toObject(User.class);
                            FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                            FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            User user = new User(null, null);
                            user.setPhone(phDialog.ccp.getSelectedCountryCodeWithPlus() + phDialog.sPhone.getText().toString().trim());
                            showRegisterDialog(user);
                        }
                    } else {
                        Toast.makeText(this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "phoneAuthCredential: ", task1.getException());
                    }
                });
            } else {
                Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                phDialog.otp.setEnabled(true);
                phDialog.progressArea.setVisibility(View.GONE);
                Log.w("TAG", "signInWithCredential:failure", task.getException());
            }
        });
    }

    private void showRegisterDialog(User user) {
        if (user.getName() != null && user.getEmail() != null && user.getPhone() != null){
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Creating Account");
            progressDialog.setMessage("Please wait while we create your account.");
            progressDialog.show();

            db.collection("Users").document(user.getUserID()).set(user).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    ME = user;
                    new FirebaseMessagingSender(this, FirebaseMessagingSender.DEFAULT_TOPICS, String.format(Locale.getDefault(), getString(R.string.user_notification_template), ME.getName()), FirebaseMessagingSender.USER_UPDATE, ME.getUserID()).sendMessage();
                    FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                    FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "showRegisterDialog: ", task.getException());
                }
            });
        } else {
            SignupLayoutBinding sDialog = SignupLayoutBinding.inflate(getLayoutInflater());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(sDialog.getRoot()).setCancelable(false);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(ActivityCompat.getDrawable(getApplicationContext(), R.drawable.dialog_bg));
            dialog.show();

            if (user.getName() != null) {
                sDialog.sName.setText(user.getName());
                sDialog.sName.setEnabled(false);
            }
            if (user.getEmail() != null) {
                sDialog.sEmail.setText(user.getEmail());
                sDialog.sEmail.setEnabled(false);
            }
            if (user.getPhone() != null) {
                sDialog.sPhone.setText(user.getPhone());
                sDialog.sPhone.setEnabled(false);
            }

            sDialog.cancel.setOnClickListener(view1 -> dialog.cancel());

            sDialog.signupBtn.setOnClickListener(view1 -> {
                if (sDialog.sName.getText().toString().trim().length() == 0) {
                    sDialog.sName.setError("Name can't be empty.");
                    return;
                }
                if (sDialog.sEmail.getText().toString().trim().length() == 0) {
                    sDialog.sEmail.setError("Email can't be empty.");
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(sDialog.sEmail.getText().toString().trim()).matches()) {
                    sDialog.sEmail.setError("Enter a valid email address.");
                    return;
                }
                if (!Patterns.PHONE.matcher(sDialog.sPhone.getText().toString()).matches()) {
                    sDialog.sPhone.setError("Enter Phone No. With Country Code.");
                    return;
                }

                user.setName(sDialog.sName.getText().toString().trim());
                user.setEmail(sDialog.sEmail.getText().toString().trim());
                user.setPhone(sDialog.ccp.getSelectedCountryCodeWithPlus()+sDialog.sPhone.getText().toString().trim());

                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Creating Account");
                progressDialog.setMessage("Please wait while we create your account.");
                progressDialog.show();

                db.collection("Users").document(user.getUserID()).set(user).addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        ME = user;
                        FirebaseMessaging.getInstance().subscribeToTopic(ME.getUserID());
                        FirebaseMessaging.getInstance().subscribeToTopic(FirebaseMessagingSender.DEFAULT_TOPICS);
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "showRegisterDialog: ", task.getException());
                    }
                });
                dialog.dismiss();
            });
        }
    }
}