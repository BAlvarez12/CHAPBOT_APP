package com.example.chapbot;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class Chatbot {
    private enum Estado {
        SALUDO,
        ESPERA_COMPRA,
        MOSTRAR_MENU,
        ESPERA_SELECCION,
        CONFIRMAR_SELECCION,
        PEDIR_NOMBRE,
        PEDIR_DIRECCION,
        PEDIR_PAGO,
        FINALIZADO
    }

    public interface RespuestaBotCallback {
        void onResponder(String respuesta, boolean pedidoCompletado, Map<String, Object> pedido);
    }

    private Estado estado = Estado.SALUDO;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String vendorId;
    private final String clientId;
    private List<MenuItem> menuItems = new ArrayList<>();
    private MenuItem seleccion;
    private final Map<String, Object> pedidoEnProceso = new HashMap<>();
    private String clienteDocId;

    public Chatbot(String vendorId, String clientId) {
        this.vendorId = vendorId;
        this.clientId = clientId;
    }

    public void procesarMensaje(String mensajeUsuario, RespuestaBotCallback cb) {
        String msg = mensajeUsuario.trim().toLowerCase(Locale.ROOT);

        switch (estado) {
            case SALUDO:
                cb.onResponder("¡Hola! Bienvenido. ¿Te gustaría ver nuestro menú?", false, null);
                estado = Estado.ESPERA_COMPRA;
                break;

            case ESPERA_COMPRA:
                if (msg.contains("si") || msg.contains("sí") || msg.contains("menu")) {
                    estado = Estado.MOSTRAR_MENU;
                    db.collection("usuario_menu")
                            .document(vendorId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    cb.onResponder("Lo siento, no encontré tu menú.", false, null);
                                    estado = Estado.ESPERA_COMPRA;
                                    return;
                                }
                                menuItems.clear();
                                String imgB64 = doc.getString("imagenBase64");
                                menuItems.add(new MenuItem(
                                        doc.getString("menu1"),
                                        doc.getDouble("precio1"),
                                        imgB64
                                ));
                                menuItems.add(new MenuItem(
                                        doc.getString("menu2"),
                                        doc.getDouble("precio2"),
                                        imgB64
                                ));
                                menuItems.add(new MenuItem(
                                        doc.getString("menu3"),
                                        doc.getDouble("precio3"),
                                        imgB64
                                ));

                                cb.onResponder("[IMAGEN_BASE64] " + imgB64, false, null);
                                String texto = String.format(
                                        Locale.getDefault(),
                                        "Nuestro menú:\n" +
                                                "1) %s — Q%.0f\n" +
                                                "2) %s — Q%.0f\n" +
                                                "3) %s — Q%.0f\n" +
                                                "Total: Q%.0f",
                                        doc.getString("menu1"), doc.getDouble("precio1"),
                                        doc.getString("menu2"), doc.getDouble("precio2"),
                                        doc.getString("menu3"), doc.getDouble("precio3"),
                                        doc.getDouble("total")
                                );
                                cb.onResponder(texto, false, null);
                                estado = Estado.ESPERA_SELECCION;
                            })
                            .addOnFailureListener(e ->
                                    cb.onResponder("Error cargando menú, inténtalo de nuevo.", false, null)
                            );
                } else {
                    cb.onResponder("Cuando quieras ver el menú, dime \"sí\".", false, null);
                }
                break;

            case ESPERA_SELECCION:
                for (MenuItem it : menuItems) {
                    if (msg.contains(it.nombre.toLowerCase(Locale.ROOT))) {
                        seleccion = it;
                        pedidoEnProceso.put("producto", it.nombre);
                        pedidoEnProceso.put("precio", it.precio);
                        estado = Estado.CONFIRMAR_SELECCION;
                        cb.onResponder(
                                "Has elegido: " + it.nombre +
                                        " (Q" + it.precio + "). ¿Confirmas?",
                                false, null
                        );
                        return;
                    }
                }
                cb.onResponder(
                        "No reconozco esa opción. Escribe el nombre exacto.",
                        false, null
                );
                break;

            case CONFIRMAR_SELECCION:
                if (msg.contains("si") || msg.contains("sí") || msg.contains("confirmo")) {
                    estado = Estado.PEDIR_NOMBRE;
                    db.collection("cliente")
                            .whereEqualTo("userId", clientId)
                            .get()
                            .addOnSuccessListener(cs -> {
                                if (!cs.isEmpty()) {
                                    clienteDocId = cs.getDocuments().get(0).getId();
                                } else {
                                    Map<String,Object> c = new HashMap<>();
                                    c.put("userId", clientId);
                                    c.put("nombre", pedidoEnProceso.get("nombreCliente"));
                                    db.collection("cliente")
                                            .add(c)
                                            .addOnSuccessListener(ref -> clienteDocId = ref.getId());
                                }
                            })
                            .addOnFailureListener(e ->
                                    cb.onResponder("Error verificando cliente.", false, null)
                            );
                    cb.onResponder("Genial. ¿Cuál es tu nombre completo?", false, null);
                } else {
                    estado = Estado.ESPERA_SELECCION;
                    cb.onResponder("Ok, dime de nuevo tu elección.", false, null);
                }
                break;

            case PEDIR_NOMBRE:
                pedidoEnProceso.put("nombreCliente", mensajeUsuario.trim());
                estado = Estado.PEDIR_DIRECCION;
                cb.onResponder("¿Cuál es tu dirección de entrega?", false, null);
                break;

            case PEDIR_DIRECCION:
                pedidoEnProceso.put("direccion", mensajeUsuario.trim());
                estado = Estado.PEDIR_PAGO;
                cb.onResponder("¿Cómo pagarás? (efectivo o tarjeta)", false, null);
                break;

            case PEDIR_PAGO:
                if (msg.contains("efectivo") || msg.contains("tarjeta")) {
                    pedidoEnProceso.put(
                            "pago",
                            msg.contains("efectivo") ? "efectivo" : "tarjeta"
                    );
                    estado = Estado.FINALIZADO;
                    Map<String,Object> d = new HashMap<>(pedidoEnProceso);
                    d.put("clienteId", clienteDocId);
                    d.put("fecha", new Date());
                    db.collection("cliente_pedido")
                            .add(d)
                            .addOnSuccessListener(ref ->
                                    cb.onResponder(
                                            "¡Listo! Tu pedido (" +
                                                    pedidoEnProceso.get("producto") +
                                                    ") se registró correctamente.",
                                            true, pedidoEnProceso
                                    )
                            )
                            .addOnFailureListener(e ->
                                    cb.onResponder("Error guardando el pedido.", false, null)
                            );
                } else {
                    cb.onResponder(
                            "Por favor escribe \"efectivo\" o \"tarjeta\".",
                            false, null
                    );
                }
                break;

            case FINALIZADO:
                cb.onResponder("Gracias por tu compra.", true, pedidoEnProceso);
                pedidoEnProceso.clear();
                menuItems.clear();
                estado = Estado.SALUDO;
                break;
        }
    }

    private static class MenuItem {
        String nombre, imagenBase64;
        double precio;
        MenuItem(String n, double p, String img) {
            this.nombre = n;
            this.precio = p;
            this.imagenBase64 = img;
        }
    }
}
