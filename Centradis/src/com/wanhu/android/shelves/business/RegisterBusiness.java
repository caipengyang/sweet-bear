package com.wanhu.android.shelves.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri.Builder;
import android.util.Log;

import com.wanhu.android.shelves.util.DateTools;
import com.wanhu.android.shelves.util.HttpManager;

public class RegisterBusiness {

	//private static final String API_REST_HOST = "192.168.0.101";
	private static final String API_REST_HOST = "centradis.c3o-digital.com";

	static final String LOG_TAG = "Shelves.Register";
	private Context mContext;
	private static final String APP_KEY = "CENTRADIS#!";
	private String VALUE_MD5;
	private static final String PARAM_MD5 = "k";
	private static final String PARAM_TABLE_NAME = "table";
	private static final String VALUE_TABLE_NAME = "register";
	private static final String JSON_TAG = "json";

	private static final String REGISTER_SUCCESS = "success";
	private static final String REGISTER_NAME = "name";
	private static final String REGISTER_SURNAME = "surname";
	private static final String REGISTER_EMAIL = "email";
	private static final String REGISTER_TEL = "telephone";
	private static final String REGISTER_COMPANY = "company";
	private static final String REGISTER_DESCRIPTION = "description";

	public RegisterBusiness(Context pContext) {
		mContext = pContext;
	}

	public boolean register(String pName, String pSurname, String pEmail,
			String pTel, String pCompany, String pDescription) {

		/*

		VALUE_MD5 = LoginBusiness.getMD5(APP_KEY + "-"
				+ DateTools.getFormatDateTime(new Date(), "MMyyyydd"));
				*/
		
		VALUE_MD5 = LoginBusiness.getMD5(APP_KEY);
		
		final Builder uri = buildRegisterQuery();
		final HttpPost post = new HttpPost(uri.build().toString());

		try {

			List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(REGISTER_NAME, pName);
			jsonObject.put(REGISTER_SURNAME, pSurname);
			jsonObject.put(REGISTER_EMAIL, pEmail);
			jsonObject.put(REGISTER_TEL, pTel);
			jsonObject.put(REGISTER_COMPANY, pCompany);
			jsonObject.put(REGISTER_DESCRIPTION, pDescription);
			nameValuePair.add(new BasicNameValuePair(JSON_TAG, jsonObject
					.toString()));
			post.setEntity(new UrlEncodedFormEntity(nameValuePair));

			return executeRequest(new HttpHost(API_REST_HOST), post);
		} catch (IOException e) {
			android.util.Log.e(LOG_TAG, "Could not perform login with query: ",
					e);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean executeRequest(HttpHost host, HttpPost post)
			throws IOException, ParseException, JSONException {
		HttpEntity entity = null;
		try {
			final HttpResponse response = HttpManager.execute(host, post);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				entity = response.getEntity();

				String _response = EntityUtils.toString(entity, HTTP.UTF_8);
				
				Log.i(LOG_TAG, _response);

				JSONObject _result = new JSONObject(_response);
				try {
					JSONObject _jsonObject = _result
							.getJSONObject(REGISTER_SUCCESS);
					if (_jsonObject != null) {
						return true;
					}
				} catch (Exception e) {
					return false;
				}

			}
		} finally {
			if (entity != null) {
				entity.consumeContent();
			}
		}
		return false;
	}

	private Builder buildRegisterQuery() {
		final Builder uri = LoginBusiness.buildGetMethod();
		uri.appendQueryParameter(PARAM_MD5, VALUE_MD5);
		uri.appendQueryParameter(PARAM_TABLE_NAME, VALUE_TABLE_NAME);
		return uri;
	}

}
