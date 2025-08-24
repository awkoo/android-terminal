package com.termux.shared.net.socket.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Base helper implementation for {@link ILocalSocketManager}. */
public abstract class LocalSocketManagerClientBase implements ILocalSocketManager {

    @Nullable
    @Override
    public Thread.UncaughtExceptionHandler getLocalSocketManagerClientThreadUEH(
        @NonNull LocalSocketManager localSocketManager) {
        return null;
    }

//    @Override
//    public void onError(@NonNull LocalSocketManager localSocketManager,
//                        @Nullable LocalClientSocket clientSocket, @NonNull Error error) {
        // Only log if log level is debug or higher since PeerCred.cmdline may contain private info
//        getLogTag();
//        if (CURRENT_LOG_LEVEL >= LOG_LEVEL_DEBUG)
//            logMessage(Log.ERROR, tag, message);
//        getLogTag();
//        LocalSocketManager.getErrorLogString(error,
//            localSocketManager.getLocalSocketRunConfig(), clientSocket);
//        if (CURRENT_LOG_LEVEL >= LOG_LEVEL_DEBUG)
//            logExtendedMessage(Log.ERROR, tag, message);
//    }

//    @Override
//    public void onDisallowedClientConnected(@NonNull LocalSocketManager localSocketManager,
//                                            @NonNull LocalClientSocket clientSocket, @NonNull Error error) {
//        getLogTag();
//        logMessage(Log.WARN, tag, message);
//        getLogTag();
//        LocalSocketManager.getErrorLogString(error,
//            localSocketManager.getLocalSocketRunConfig(), clientSocket);
//        logExtendedMessage(Log.WARN, tag, message);
//    }

    @Override
    public void onClientAccepted(@NonNull LocalSocketManager localSocketManager,
                                 @NonNull LocalClientSocket clientSocket) {
        // Just close socket and let child class handle any required communication
        clientSocket.closeClientSocket(true);
    }



//    protected abstract String getLogTag();

}
