/*
 * File: ServerRegistrationActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */

package org.hfoss.posit;

import java.util.List;

import org.apache.commons.validator.EmailValidator;
import org.hfoss.posit.utilities.Utils;
import org.hfoss.posit.web.Communicator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Prompts the user to register their phone if the phone is not registered, or
 * shows the phone's current registration status and allows the user to register
 * their phone again with a different server.
 */
public class RegisterUserActivity extends Activity implements OnClickListener {

	private Button registerButton;
	private SharedPreferences sp;
	private ProgressDialog mProgressDialog;
	private static final int CREATE_ACCOUNT = 1;
	private static final String TAG = "RegisterUserActivity";

	/**
	 * Called when the Activity is first started. If the phone is not
	 * registered, tells the user so and gives the user instructions on how to
	 * register the phone. If the phone is registered, tells the user the server
	 * address that the phone is registered to in case the user would like to
	 * change it.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		String server = i.getStringExtra("server");
		String email = i.getStringExtra("email");
		setContentView(R.layout.registeruser);
		((TextView) findViewById(R.id.serverName)).setText(server);
		((TextView) findViewById(R.id.email)).setText(email);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		registerButton = (Button) findViewById(R.id.submitInfo);
		registerButton.setOnClickListener(this);

	}

	/**
	 * Method that handles when user clicks on create account button (after
	 * having filled out the server user information)
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		case (R.id.submitInfo):
			Intent i = getIntent();
			EmailValidator emV = EmailValidator.getInstance();
			String password = (((TextView) findViewById(R.id.password))
					.getText()).toString();
			String check = (((TextView) findViewById(R.id.passCheck)).getText())
					.toString();
			String email = (((TextView) findViewById(R.id.email)).getText())
					.toString();
			String lastname = (((TextView) findViewById(R.id.lastName))
					.getText()).toString();
			String firstname = (((TextView) findViewById(R.id.firstName))
					.getText()).toString();
			String server = i.getStringExtra("server");
			if (password.equals("") || check.equals("") || lastname.equals("")
					|| firstname.equals("") || email.equals("")) {
				Utils.showToast(registerButton.getContext(),
						"Please fill in all the fields");
				break;
			}

			if (emV.isValid(email) != true) {
				Utils.showToast(registerButton.getContext(),
						"Please enter a valid email address");
				break;
			}
			if (!check.equals(password)) {
				Utils.showToast(registerButton.getContext(),
						"Your passwords do not match");
				break;
			}

			TelephonyManager manager = (TelephonyManager) this
					.getSystemService(Context.TELEPHONY_SERVICE);
			String imei = manager.getDeviceId();

			Communicator com = new Communicator(registerButton.getContext());
			mProgressDialog = ProgressDialog.show(this, "Registering device",
					"Please wait.", true, true);
			String result = com.registerUser(server, firstname, lastname,
					email, password, check, imei);
			Log.i(TAG, result);
			if (result != null) {
				String[] message = result.split(":");
				if (message.length != 2) {
					Utils.showToast(this, "Malformed message");
					break;
				}
				if (message[0].equals("" + Constants.AUTHN_OK)) {
					Intent data = new Intent();
					data.putExtra("email", email);
					data.putExtra("password", password);
					setResult(CREATE_ACCOUNT, data);
					finish();
				} else {
					Utils.showToast(this, message[1]);
				}
				break;

			}
			mProgressDialog.dismiss();

		}
	}
}