package com.example.chapbot;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Chatbot {

    private int estadoConversacion = 0;
    private Map<String, Object> pedidoEnProceso = new HashMap<>();

    public interface RespuestaBotCallback {
        void onResponder(String respuesta, boolean pedidoCompletado, Map<String, Object> pedido);
    }

    public void procesarMensaje(String mensajeUsuario, RespuestaBotCallback callback) {
        String respuesta;

        mensajeUsuario = mensajeUsuario.toLowerCase(Locale.ROOT);

        switch (estadoConversacion) {
            case 0:
                respuesta = "¡Hola! Bienvenido a FastBot 🍔 ¿Qué te gustaría ordenar hoy?";
                estadoConversacion = 1;
                break;

            case 1:
                if (mensajeUsuario.contains("hamburguesa")) {
                    pedidoEnProceso.put("producto", "hamburguesa");
                    respuesta = "¿Deseas con queso, sencilla o doble carne?";
                    estadoConversacion = 2;
                } else {
                    respuesta = "Por ahora solo tengo hamburguesas 🍔 ¿Quieres una?";
                }
                break;

            case 2:
                if (mensajeUsuario.contains("queso")) {
                    pedidoEnProceso.put("producto", "hamburguesa con queso");
                } else if (mensajeUsuario.contains("doble")) {
                    pedidoEnProceso.put("producto", "hamburguesa doble carne");
                } else {
                    pedidoEnProceso.put("producto", "hamburguesa sencilla");
                }
                respuesta = "¿Deseas agregar papas y bebida por Q10 más?";
                estadoConversacion = 3;
                break;

            case 3:
                if (mensajeUsuario.contains("sí") || mensajeUsuario.contains("si")) {
                    pedidoEnProceso.put("extra", "papas y bebida");
                } else {
                    pedidoEnProceso.put("extra", "ninguno");
                }
                respuesta = "Perfecto. ¿Cuál es tu dirección de entrega?";
                estadoConversacion = 4;
                break;
            case 4:
                pedidoEnProceso.put("direccion", mensajeUsuario);
                pedidoEnProceso.put("estado", "en_proceso");
                pedidoEnProceso.put("fecha", new Date());
                respuesta = "¡Tu pedido ha sido recibido y está en camino! 🚗💨 Gracias por confiar en FastBot.";
                estadoConversacion = 0;
                callback.onResponder(respuesta, true, pedidoEnProceso);
                pedidoEnProceso = new HashMap<>();
                return;

            default:
                respuesta = "¿En qué te puedo ayudar?";
                estadoConversacion = 0;
                break;
        }

        callback.onResponder(respuesta, false, null);
    }
}
