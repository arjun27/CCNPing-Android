/*
 * Simple ping client and server
 * Takes a prefix and runs at prefix/ping
 * 
 * Add:
 * 	Ping interval, total number of pings for client
 * 	Freshness seconds for server
 */

package org.irl.ccnping;

import org.ccnx.ccn.protocol.MalformedContentNameStringException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {

	// Debugging
	private final static boolean D = true;
	private final static String TAG = "CCNPing";

	// Declare graphical elements
	private TextView _status;
	private Button _serverButton;
	private Button _clientButton;
	private EditText _prefixAddress;
	private TextView _results;
	private Context _ctx;
	private boolean _started = false;

	private CCNPingHelper _helper;

	// Defaults
	private String _defaultPrefix = "ccnx:/ndn/ccnping/";

	// Handler
	private Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String text = (String)msg.obj;
			_results.append(text + "\n");
			msg.obj = null;
			msg = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (D) Log.v(TAG, " --- ON CREATE --- ");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		_status 	= (TextView)findViewById(R.id.status);
		_serverButton = (Button)findViewById(R.id.server_button);
		_clientButton = (Button)findViewById(R.id.client_button);
		_prefixAddress = (EditText)findViewById(R.id.prefix);
		_results	= (TextView)findViewById(R.id.results);

		_prefixAddress.setText(_defaultPrefix);

		_serverButton.setOnClickListener(this);
		_clientButton.setOnClickListener(this);

		_ctx = this.getApplicationContext();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.server_button:
			if (D) Log.v(TAG, " ---- server onClick");
			try {
				if (!_started) {
					_helper = new CCNPingHelper(_ctx, _prefixAddress.getText().toString(), _handler, true);
					_helper.start();
					_serverButton.setText("Stop Server");
					_started = true;
				} else {
					_helper.cancel();
					_helper = null;
					_started = false;
					_serverButton.setText("Start Server");
				}
			} catch (MalformedContentNameStringException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		case R.id.client_button:
			if (D) Log.v(TAG, " ---- client onClick");
			try {
				if (!_started) {
					_helper = new CCNPingHelper(_ctx, _prefixAddress.getText().toString(), _handler, false);
					_helper.start();
					_clientButton.setText("Stop Client");
					_started = true;
				} else {
					_helper.cancel();
					_helper = null;
					_started = false;
					_serverButton.setText("Start Client");
				}
			} catch (MalformedContentNameStringException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
	}

	@Override
	protected void onDestroy() {
		if (D) Log.v(TAG, "--- ON DESTROY ---");
		super.onDestroy();
		if (_helper != null) _helper.cancel();
	}
}
