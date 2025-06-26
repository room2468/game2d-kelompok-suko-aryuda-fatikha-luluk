package auth;

import java.util.HashMap;

public class AuthHandler {
    private HashMap<String, Boolean> authenticatedPlayers;

    public AuthHandler() {
        this.authenticatedPlayers = new HashMap<>();
    }

    public boolean login(String ipAddress) {
        if (isValidIPAddress(ipAddress)) {
            System.out.println("IP valid: " + ipAddress);
            authenticatedPlayers.put(ipAddress, true);
            return true;
        }
        System.out.println("IP tidak valid: " + ipAddress);
        return false;
    }

    private boolean isValidIPAddress(String ipAddress) {
        return ipAddress.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
    }

    public boolean isAuthenticated(String ipAddress) {
        return authenticatedPlayers.getOrDefault(ipAddress, false);
    }
}
