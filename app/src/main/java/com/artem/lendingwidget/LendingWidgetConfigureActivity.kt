package com.artem.lendingwidget


import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.artem.lendingwidget.extensions.getUrl
import com.artem.lendingwidget.extensions.storeUrl
import com.artem.lendingwidget.network.LendingNetworkService

class LendingWidgetConfigureActivity : Activity() {
    internal var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    internal lateinit var mUrlEditText: EditText
    internal lateinit var mConnectButton: Button
    internal lateinit var mProgressBar: ProgressBar

    internal val mBroadCastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
           this@LendingWidgetConfigureActivity.onReceive(context, intent)
        }
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(Activity.RESULT_CANCELED)

        setContentView(R.layout.lending_widget_configure)
        mUrlEditText = findViewById(R.id.et_url) as EditText
        mConnectButton = findViewById(R.id.btn_connect) as Button
        mProgressBar = findViewById(R.id.progressBar) as ProgressBar


        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        mConnectButton.setOnClickListener { tryConnection()}
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadCastReceiver, IntentFilter(LendingNetworkService.ACTION_RESULT))

        var url = this.getUrl(mAppWidgetId)
        mUrlEditText.setText(url)
    }

    private fun toggleViews(active: Boolean) {
        if (active) {
            mUrlEditText.isEnabled = true
            mConnectButton.isEnabled = true
            mProgressBar.visibility = View.GONE
        } else {
            mUrlEditText.isEnabled = false
            mConnectButton.isEnabled = false
            mProgressBar.visibility = View.VISIBLE
        }
    }

    internal fun tryConnection() {
        toggleViews(false)

        // TODO handle http/https
        this.storeUrl("${mUrlEditText.text}", mAppWidgetId)
        LendingNetworkService.updateBotlog(this, true, mAppWidgetId)
        LendingNetworkService.updateBpi(this, false, mAppWidgetId)
    }

    internal fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            return
        }

        toggleViews(true)

        val success = intent.getBooleanExtra(LendingNetworkService.RESULT_SUCCESS, false)
        val originalAction = intent.getStringExtra(LendingNetworkService.RESULT_ACTION)
        val widgetId = intent.getIntExtra(LendingNetworkService.RESULT_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if ((originalAction != LendingNetworkService.ACTION_BOTLOG) || (widgetId != mAppWidgetId)) {
            return
        }

        if (success) {
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            this.finish()
        } else {
            Toast.makeText(this, getString(R.string.error_connection, mUrlEditText.text), Toast.LENGTH_LONG).show()
        }
    }

    companion object {

        internal fun startIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent()
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .setClass(context, LendingWidgetConfigureActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}

