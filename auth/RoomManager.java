package auth;

import java.util.*;

public class RoomManager {
    private Map<String, List<String>> rooms; // Map roomID ke daftar pemain
    private static final int MAX_PLAYERS_PER_ROOM = 3;

    public RoomManager() {
        this.rooms = Collections.synchronizedMap(new HashMap<>());
    }

    // Membuat room baru
    public synchronized String createRoom() {
        String roomID = "Room-" + (rooms.size() + 1);
        rooms.put(roomID, Collections.synchronizedList(new ArrayList<>()));
        System.out.println("Room baru dibuat: " + roomID);
        return roomID;
    }

    // Menambahkan pemain ke room yang sudah ada atau membuat room baru jika semua penuh
    public synchronized String addPlayerToRoom(String ipAddress) {
        for (Map.Entry<String, List<String>> entry : rooms.entrySet()) {
            if (entry.getValue().size() < MAX_PLAYERS_PER_ROOM) {
                entry.getValue().add(ipAddress);
                System.out.println("Pemain " + ipAddress + " ditambahkan ke " + entry.getKey());
                return entry.getKey();
            }
        }

        // Jika semua room penuh, buat room baru
        String newRoomID = createRoom();
        rooms.get(newRoomID).add(ipAddress);
        System.out.println("Pemain " + ipAddress + " ditambahkan ke room baru: " + newRoomID);
        return newRoomID;
    }

    public synchronized void removePlayer(String ipAddress) {
        boolean removed = false;
    
        // Cari pemain di setiap room dan hapus
        for (Map.Entry<String, List<String>> entry : rooms.entrySet()) {
            List<String> playersInRoom = entry.getValue();
            if (playersInRoom.remove(ipAddress)) {
                removed = true;
                System.out.println("Pemain " + ipAddress + " dihapus dari " + entry.getKey());
                break;
            }
        }
    
        if (!removed) {
            System.err.println("Pemain tidak ditemukan: " + ipAddress);
        }
    }
    
    
    // Menghapus pemain dari room tertentu
    public synchronized void removePlayerFromRoom(String roomID, String ipAddress) {
        List<String> playersInRoom = rooms.get(roomID);
    
        if (playersInRoom != null && playersInRoom.remove(ipAddress)) {
            System.out.println("Pemain " + ipAddress + " dihapus dari room " + roomID);
            // Hapus room jika kosong
            if (playersInRoom.isEmpty()) {
                rooms.remove(roomID);
                System.out.println("Room " + roomID + " dihapus karena kosong.");
            }
        } else {
            System.err.println("Pemain " + ipAddress + " tidak ditemukan di room " + roomID);
        }
    }
    
    

    // Memeriksa apakah room tertentu siap untuk memulai permainan
    public synchronized boolean isRoomReady(String roomID) {
        List<String> players = rooms.get(roomID);
        return players != null && players.size() >= MAX_PLAYERS_PER_ROOM;
    }

    // Mendapatkan jumlah pemain dalam room tertentu
    public synchronized int getPlayerCount(String roomID) {
        List<String> players = rooms.get(roomID);
        return players == null ? 0 : players.size();
    }

    // Mendapatkan semua room
    public synchronized Map<String, List<String>> getRooms() {
        return new HashMap<>(rooms);
    }
}
