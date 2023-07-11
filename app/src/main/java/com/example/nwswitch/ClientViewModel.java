package com.example.nwswitch;

import androidx.lifecycle.*;
import android.widget.TextView;
import android.text.Layout;

import java.util.ArrayList;
import static java.lang.Integer.max;

public class ClientViewModel extends ViewModel {
    private MutableLiveData<String> connectionInfo;
    private MutableLiveData<ArrayList<LogLine>> logBuffer;

    private String title = "";

    public LiveData<String> getConnectionInfo() {
        if (connectionInfo == null) {
            connectionInfo = new MutableLiveData<>();
        }
        return connectionInfo;
    }

    public LiveData<ArrayList<LogLine>> getLogBuffer() {
        if (logBuffer == null) {
            logBuffer = new MutableLiveData<>();
        }
        return logBuffer;
    }

    public void updateConnectionInfo(String newConnectionInfo) {
        connectionInfo.postValue(newConnectionInfo);
    }

    public void updateLogBuffer(ArrayList<LogLine> newLogBuffer) {
        logBuffer.postValue(newLogBuffer);
    }

    public void linkToMyViews(androidx.lifecycle.LifecycleOwner owner,
                              String _title, TextView infoView, TextView logView) {
        title = _title;

        getConnectionInfo().observe(owner, connectionInfo -> {
            StringBuilder sb = new StringBuilder();
            sb.append(title).append(" ").append(connectionInfo);
            infoView.setText(sb.toString());
        });

        final int logScrollLines = 64;
        getLogBuffer().observe(owner, logBuffer -> {
            StringBuilder sb = new StringBuilder(); int s = logBuffer.size();
            for (LogLine logLine : logBuffer.subList(max(s - logScrollLines, 0), s)) {
                sb.append(logLine.text).append("\n");
            }
            logView.setText(sb.toString());
            logView.post(() -> {
                Layout layout = logView.getLayout();
                int lineCount = logView.getLineCount(), height = logView.getHeight();
                if (layout != null && lineCount > 0) {
                    int lineTop = layout.getLineTop(lineCount - 1);
                    logView.scrollTo(0, max(lineTop - height, 0));
                }
            });
        });
    }
}