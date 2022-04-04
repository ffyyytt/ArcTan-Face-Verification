package com.example.demofaceidapp;

import android.app.Application;
import android.util.Log;
import android.util.SparseArray;

import com.example.demofaceidapp.data.Face;
import com.example.demofaceidapp.data.FaceData;
import com.example.demofaceidapp.data.User;

import java.util.ArrayList;
import java.util.List;

import io.paperdb.Paper;

public class MainApplication extends Application {

    public static final String KEY_USERS = "KEY_USERS";
    public List<User> users;
    public SparseArray<User> mapUsers;

    @Override
    public void onCreate() {
        super.onCreate();
        Paper.init(this);
        users = Paper.book().read(KEY_USERS, new ArrayList<>());
        updateMapUsers();
        Log.d("onCreate", "Update users");
    }

    private void updateMapUsers() {
        if (mapUsers == null) mapUsers = new SparseArray<>();
        for (int i = 0; i < users.size(); i++) {
            mapUsers.put(users.get(i).id, users.get(i));
        }
    }

    private void saveUserList() {
        Paper.book().write(KEY_USERS, users);
    }

    public User getUser(int id) {
        return mapUsers.get(id);
    }

    public User addNewUser(String name) {
        if (users == null) users = new ArrayList<>();
        User user = new User();
        int nextId = 0;
        if (users.size() > 0) {
            nextId = users.get(users.size() - 1).id + 1;
        }
        user.id = nextId;
        user.name = name;
        users.add(user);
        saveUserList();
        updateMapUsers();
        return user;
    }

    public void deleteUser(int userId) {
        if (users == null) return;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id == userId) {
                users.remove(i);
                saveUserList();
                updateMapUsers();
                return;
            }
        }
    }

    public void addFaces(int userId, List<Face> faces) {
        if (faces == null) return;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id == userId) {
                if (users.get(i).faces == null) users.get(i).faces = new ArrayList<>();
                users.get(i).faces.addAll(faces);
                saveUserList();
                return;
            }
        }
    }

    public List<FaceData> getFaceData() {
        if (users == null) return null;
        List<FaceData> data = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).faces == null) continue;
            for (int j = 0; j < users.get(i).faces.size(); j++) {
                data.add(new FaceData(users.get(i).id, users.get(i).faces.get(j)));
            }
        }
        return data;
    }
}
