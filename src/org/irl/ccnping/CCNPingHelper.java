package org.irl.ccnping;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ccnx.android.ccnlib.CCNxConfiguration;
import org.ccnx.android.ccnlib.CCNxServiceCallback;
import org.ccnx.android.ccnlib.CCNxServiceControl;
import org.ccnx.android.ccnlib.CCNxServiceStatus.SERVICE_STATUS;
import org.ccnx.android.ccnlib.CcndWrapper.CCND_OPTIONS;
import org.ccnx.android.ccnlib.RepoWrapper.REPO_OPTIONS;
import org.ccnx.ccn.CCNContentHandler;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.ccnx.ccn.protocol.SignedInfo;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class CCNPingHelper implements Runnable, CCNxServiceCallback, CCNInterestHandler, CCNContentHandler {

	// Debugging
	private final static boolean D = true;
	private final static String TAG = "CCNPing";

	// Defaults
	private String PING_COMPONENT = "ping";

	private Context _ctx;
	private Thread _thd;
	private ContentName _prefix;
	private Handler _handler;
	private boolean _isServer;
	private CCNxServiceControl _ccnxService;

	// Ping variables
	private int sent = 0;
	private int received = 0;
	private float totalRtt = 0;
	private boolean stop = false;


	private CCNHandle _handle;

	public CCNPingHelper(Context ctx, String prefix, Handler handler, boolean isServer) 
			throws MalformedContentNameStringException {
		this._ctx = ctx;
		_handler = handler;
		_isServer = isServer;

		if (prefix.charAt(prefix.length() - 1) != '/') {
			prefix = prefix + '/';
			if (D) Log.v(TAG, "Prefix is " + prefix);
		}
		_prefix = ContentName.fromURI(prefix + PING_COMPONENT);

		CCNxConfiguration.config(ctx, false);
		_thd = new Thread(this, "CCNPingHelper");
	}

	public void start() {
		_thd.start();
	}

	@Override
	public void newCCNxStatus(SERVICE_STATUS st) {
		switch(st) {
		case START_ALL_DONE:
			Log.i(TAG, "CCNx Services is ready");
			break;
		case START_ALL_ERROR:
			Log.i(TAG, "CCNx Services are not ready");
			break;
		}
	}

	@Override
	public void run() {
		if (initializeCCNx()) {
			Log.v(TAG, "CCNx initialized");
			_handler.obtainMessage(0,0,-1, "CCN Initialized").sendToTarget();
			try {
				_handle = CCNHandle.open();
				_handle.registerFilter(_prefix, this);

				if (_isServer) {
					// TODO
				} else {
					while (!stop) {
						Date now = new Date();
						String suffix = String.valueOf(now.getTime());
						Interest interest = new Interest(_prefix.append(suffix));
						_handle.expressInterest(interest, this);
						Log.v(TAG, "Sent interest " + suffix);
						sent++;
						Thread.sleep(1000);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "Some error in CCNHandle");
			}

		} else {
			Log.e(TAG, "Cannot initialize CCNx");
		}
	}

	protected boolean initializeCCNx() {
		_ccnxService = new CCNxServiceControl(_ctx);
		_ccnxService.registerCallback(this);
		_ccnxService.setCcndOption(CCND_OPTIONS.CCND_DEBUG, "1");
		_ccnxService.setRepoOption(REPO_OPTIONS.REPO_DEBUG, "WARNING");
		return _ccnxService.startAll();
	}

	@Override
	public boolean handleInterest(Interest interest) {
		if (_isServer) {
			if (D) Log.v(TAG, "Handling interest as server");

			// Check if interest is valid
			if (D) Log.v(TAG, interest.toString());

			// Create ContentObject
			ContentObject co;
			try {
				co = new ContentObject(interest.getContentName(), 
						new SignedInfo(_handle.getDefaultPublisher(), _handle.keyManager().getDefaultKeyLocator()), 
						null, _handle.keyManager().getDefaultSigningKey());
				_handle.put(co);
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "InvalidKeyException");
				e.printStackTrace();
			} catch (SignatureException e) {
				Log.e(TAG, "SignatureException");
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "IOException");
				e.printStackTrace();
			}
		} else {
			if (D) Log.v(TAG, "Handling interest as client");
			// Nothing to do
		}
		return false;
	}

	@Override
	public Interest handleContent(ContentObject co, Interest interest) {
		if (_isServer) {
			if (D) Log.v(TAG, "Handling content object as server");
			// Nothing to do
		} else {
			if (D) Log.v(TAG, "Handling content object as client");
			String coName = co.getContentName().toString();
			String suffix = coName.substring(coName.lastIndexOf('/') + 1);
			_handler.obtainMessage(0,0,-1, "Received " + suffix).sendToTarget();
			received++;
			Date nowDate = new Date();
			Long now = nowDate.getTime();
			Long then = Long.parseLong(suffix);
			Long rtt = now - then;
			totalRtt += rtt;
			_handler.obtainMessage(0,0,-1, "RTT " + rtt.toString() + "ms").sendToTarget();
		}
		return null;
	}
	
	private void summary() {
		_handler.obtainMessage(0,0,-1, "Sent " + sent).sendToTarget();
		_handler.obtainMessage(0,0,-1, "Received " + received).sendToTarget();
		_handler.obtainMessage(0,0,-1, "Average RTT " + (totalRtt / received)).sendToTarget();
	}

	public void cancel() {
		//TODO: sometimes unregistering is useless, and you have to force quit manually
		if (_handle != null && _prefix != null) {
			stop = true;
			summary();
			_handler.obtainMessage(0,0,-1, "Stopped").sendToTarget();
			Log.i(TAG, "Unregistering namespace " + _prefix.toURIString());
			//_handle.unregisterFilter(_prefix, this);
			_handle.close();
			_handle = null;
		}
	}
}
