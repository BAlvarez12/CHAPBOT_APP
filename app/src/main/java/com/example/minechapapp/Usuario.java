package com.example.minechapapp;

public class Usuario {
    private String uid;
    private String nombre;
    private String email;

    public Usuario(String uid, String nombre, String email) {
        this.uid = uid;
        this.nombre = nombre;
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }
}
