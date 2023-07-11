package com.example.nwswitch;

import android.view.View;

// single log buffer entry; made identifiable to support the case where,
// (1) a TextView (inside a layout container) is used for each log line, and
// (2) one wants to implement an efficient re-rendering for general log buffer updates, e.g.,
// when some log lines in the middle are removed
public class LogLine {
    int id;
    String text;

    public LogLine(String _text) {
        id = View.generateViewId();
        text = _text;
    }
}
