package com.example.nwswitch;

import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;

import android.net.*;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Client {
    private static final String logTag = "NWSwitch";

    private static final String host = "tcpbin.com";
    private static final int port = 4242;
    private static final long echoInterval = 1000; // milliseconds
    private final String echoMessage;

    private final int transportType;

    private final HashSet<Long> availableNetworks = new HashSet<>();
    private Socket socket = null;

    private String connectionInfo;
    private static final int logBufferSize = 1024;
    private final ArrayList<LogLine> logBuffer = new ArrayList<>();

    private final ClientViewModel viewModel;

    public Client(ConnectivityManager cm, int _transportType, ClientViewModel _viewModel) {
        transportType = _transportType; viewModel = _viewModel;

        switch (transportType) {
            case TRANSPORT_WIFI:     echoMessage = "On Wi-Fi"; break;
            case TRANSPORT_CELLULAR: echoMessage = "On cellular"; break;
            default:                 echoMessage = "On one of them"; break;
        }
        viewModel.updateConnectionInfo(connectionInfo = "");

        NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
        if (transportType >= 0) requestBuilder.addTransportType(transportType);
        // without the NET_CAPABILITY_INTERNET requirement, powering Wi-Fi on/off disrupts cellular
        // only connections: the existing cellular only network gets lost and a new network
        // (with different network ID and uplink/downlink speeds) is made available on cellular
        requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        NetworkRequest networkRequest = requestBuilder.build();

        // starting with API level 31, objects do not contain any location sensitive information
        // by default (even if app holds the required permissions) -- this frees the system from
        // tracking app location usage (just because it has location access permission); with new
        // model app should request location sensitive information to be included in the object
        // explicitly (thorough FLAG_INCLUDE_LOCATION_INFO), in which case system will check
        // location permission and the location toggle state, and if all is met will take note of
        // app location usage only if any such information is returned
        // https://developer.android.com/reference/android/net/ ..
        // .. ConnectivityManager.NetworkCallback.html#FLAG_INCLUDE_LOCATION_INFO

        // ConnectivityManager.NetworkCallback older constructor (level < 31) takes no argument,
        // and its new incompatible constructor takes an integer (flags) argument; this poor API
        // design requires implementing both versions (under a Build.VERSION.SDK_INT check), and
        // even worse the whole long body (anonymous inner class) that overrides network callbacks
        // needs to be copied for each constructor (or define a separate named class),
        //    networkCallback = new ConnectivityManager.NetworkCallback() { @override ... }
        //    networkCallback = new ConnectivityManager.NetworkCallback(flag) { @override ... }
        // we just keep the second copy here for API level >= 31; for lower API levels comment out
        // or remove the the flag argument below and rebuild (code will crash otherwise)

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.
                NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
            // NetworkCallback class is extended (not inherited), i.e., respective parent
            // callbacks (super.xyz()) need to be called; see https://developer.android.com/ ..
            // .. training/monitoring-device-state/connectivity-status-type#ConfigureCallback
            @Override
            public void onAvailable(Network network) {
                availableNetworks.add(network.getNetworkHandle());
                super.onAvailable(network);
            }

            // https://developer.android.com/reference/android/net/
            // ConnectivityManager.NetworkCallback.html#onUnavailable()
            //
            // Called if no network is found within the timeout time specified in the
            // ConnectivityManager.requestNetwork(android.net.NetworkRequest,
            // android.net.ConnectivityManager.NetworkCallback, int) call, or if the requested
            // network characteristics cannot be fulfilled (whether or not a timeout was specified);
            // by the time this callback is invoked the associated NetworkRequest has been
            // removed and released, as if ConnectivityManager.unregisterNetworkCallback(
            // android.net.ConnectivityManager.NetworkCallback) had been called.
            @Override
            public void onUnavailable() {
                log("No network found; operation stopped.");
                viewModel.updateConnectionInfo(connectionInfo = "");
                super.onUnavailable();
            }

            @Override
            public void onCapabilitiesChanged(Network network,
                                              NetworkCapabilities capabilities) {
                String fName = "NetworkCallback() -> onCapabilitiesChanged(): ";
                Log.i(logTag, fName + ((network != null && capabilities != null) ?
                        "Network " + network + " capabilities: " + capabilities :
                        "No network or capabilities"));

                // on CapabilitiesChanged() is called frequently, e.g., with Wi-Fi rssi changes;
                // note a change to location access permission is not a network capability change,
                // e.g., allowing location access (to gives access to ssid and bssid) while app is
                // running does -not- invoke this callback: in this case, ssid and bssid are
                // exposed naturally at the next callback made in response to events such as an
                // rssi change; however, disallowing location access while the app is running
                // instantly masks getSSID() and getBSSID() outputs

                // interestingly, fresh (synchronous) network, network capabilities, and transport
                // info reads outside the callback (network = cm.getActiveNetwork(), capabilities =
                // network.getNetworkCapabilities() ...) do not expose ssid and bssid, even with
                // location access permission, showing emphasis on move to asynchronous model
                connectionInfo = ""; TransportInfo transportInfo;
                if (network != null && capabilities != null &&
                        (transportInfo = capabilities.getTransportInfo()) != null) {
                    // Wi-Fi
                    if (transportType == TRANSPORT_WIFI && transportInfo instanceof WifiInfo) {
                        WifiInfo wifiInfo = (WifiInfo) transportInfo;
                        connectionInfo = String.format(Locale.US,
                                "| SSID: %s\nBSSID: %s, RSSI: %d dBm",
                                wifiInfo.getSSID(), wifiInfo.getBSSID(), wifiInfo.getRssi());
                    }
                    // cellular
                    // no support for cellular at this point, only Wi-Fi and Wi-Fi Aware
                    // https://developer.android.com/reference/android/net/TransportInfo
                    // else if (transportType == TRANSPORT_CELLULAR && ) { ... }
                }
                viewModel.updateConnectionInfo(connectionInfo); // publish to view model
                super.onCapabilitiesChanged(network, capabilities);
            }

            @Override
            public void onLost(Network network) {
                availableNetworks.remove(network.getNetworkHandle());
                viewModel.updateConnectionInfo(connectionInfo = "");
                super.onLost(network);
            }
        };
        // providing a timeout value (in milliseconds, third argument) will invoke the
        // onUnavailable() if network cannot be established in time
        cm.requestNetwork(networkRequest, networkCallback);

        // echo timer
        Timer timer = new Timer();
        // isn't this valid? TimerTask timerTask = () -> { echo(); };
        TimerTask timerTask = new TimerTask() { public void run() { echo(); } };
        timer.schedule(timerTask, 0, echoInterval);
    }

    private void log(String logMessage) {
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss", Locale.US);
        String time = dateFormat.format(Calendar.getInstance().getTime());
        String text = time + " " + logMessage;
        logBuffer.add(new LogLine(text));
        if (logBuffer.size() > logBufferSize)
            logBuffer.subList(0, logBuffer.size() - logBufferSize).clear();
        viewModel.updateLogBuffer(logBuffer); // publish to view model
    }

    public void echo() {
        String fName = "echo(): ";
        if (socket == null && !availableNetworks.isEmpty()) {
            long handle = availableNetworks.iterator().next();
            Network network = Network.fromNetworkHandle(handle);
            try {
                socket = new Socket(); network.bindSocket(socket);
                socket.connect(new InetSocketAddress(host, port));
            } catch (IOException e) {
                Log.i(logTag, fName + e);
                socket = null;
            }
        }
        if (socket != null) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String message = echoMessage + "\n"; // carriage return to prompt echo response
                outputStream.write(message.getBytes()); outputStream.flush();
                String response = bufferedReader.readLine().replace("\n", "");
                log("echo: " + response);
            } catch (IOException e1) {
                Log.i(logTag, fName + e1);
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.i(logTag, fName + e2);
                }
                socket = null;
            }
        }
    }
}
