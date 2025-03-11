package com.example.minechapapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragmento extends Fragment {

    private RecyclerView recyclerView;
    private ChatAdap chatAdap;
    private List<Chat_individual> listaDeChats;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragmento_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaDeChats = obtenerChatsSimulados();

        chatAdap = new ChatAdap(listaDeChats);
        recyclerView.setAdapter(chatAdap);

        return view;
    }

    private List<Chat_individual> obtenerChatsSimulados() {
        List<Chat_individual> chats = new ArrayList<>();
        chats.add(new Chat_individual("Juan Pérez", "¡Hola! ¿Cómo estás?", "10:30 AM"));
        chats.add(new Chat_individual("Ana Gómez", "Nos vemos luego", "09:15 AM"));
        chats.add(new Chat_individual("Carlos López", "¿Qué planes para hoy?", "Ayer"));
        chats.add(new Chat_individual("Laura Sánchez", "Feliz cumpleaños 🎉", "Domingo"));
        chats.add(new Chat_individual("Pedro Ramírez", "Envié los documentos", "Sábado"));
        return chats;
    }
}
