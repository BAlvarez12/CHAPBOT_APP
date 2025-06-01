package com.example.chapbot;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class Chatbot {
    private enum Estado {
        SALUDO,
        ESPERA_COMPRA,
        MOSTRAR_MENU,
        ESPERA_SELECCION,
        CONFIRMAR_SELECCION,
        ESPERA_OTRO_COMBO,
        PEDIR_NOMBRE,
        PEDIR_DIRECCION,
        PEDIR_PAGO,
        PEDIR_OBSERVACION,
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
    private final List<MenuItem> itemsSeleccionados = new ArrayList<>();
    private double total = 0.0;
    private String clienteDocId;

    public Chatbot(String vendorId, String clientId) {
        this.vendorId = vendorId;
        this.clientId = clientId;
    }

    public void procesarMensaje(String mensajeUsuario, RespuestaBotCallback cb) {
        String msg = mensajeUsuario.trim().toLowerCase(Locale.ROOT);

        switch (estado) {
            case SALUDO:
                cb.onResponder("👋 ¡Hola! Bienvenido. ¿Deseas ver nuestro menú del día? (sí/no)", false, null);
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
                                    cb.onResponder("😔 Lo siento, no encontré tu menú.", false, null);
                                    estado = Estado.ESPERA_COMPRA;
                                    return;
                                }
                                menuItems.clear();
                                String imgB64 = doc.getString("imagenBase64");
                                menuItems.add(new MenuItem(doc.getString("menu1"), doc.getDouble("precio1"), imgB64));
                                menuItems.add(new MenuItem(doc.getString("menu2"), doc.getDouble("precio2"), imgB64));
                                menuItems.add(new MenuItem(doc.getString("menu3"), doc.getDouble("precio3"), imgB64));

                                // Usamos exactamente "[IMAGEN_BASE64] " como prefijo para que coincida con el 'tag' en chatActivity
                                cb.onResponder("[IMAGEN_BASE64] " + imgB64, false, null);

                                String texto = String.format(
                                        Locale.getDefault(),
                                        "📋 Nuestro menú del día:\n" +
                                                "1) %s — Q%.0f\n" +
                                                "2) %s — Q%.0f\n" +
                                                "3) %s — Q%.0f\n\n" +
                                                "Escribe el número (1, 2 o 3) del platillo que deseas.",
                                        doc.getString("menu1"), doc.getDouble("precio1"),
                                        doc.getString("menu2"), doc.getDouble("precio2"),
                                        doc.getString("menu3"), doc.getDouble("precio3")
                                );
                                cb.onResponder(texto, false, null);
                                estado = Estado.ESPERA_SELECCION;
                            })
                            .addOnFailureListener(e ->
                                    cb.onResponder("❌ Error cargando menú, inténtalo de nuevo.", false, null)
                            );
                } else {
                    cb.onResponder("👍 Cuando quieras, solo dime \"sí\" para ver el menú del día.", false, null);
                }
                break;

            case ESPERA_SELECCION:
                int opcion;
                try {
                    opcion = Integer.parseInt(msg.trim());
                } catch (NumberFormatException e) {
                    opcion = -1;
                }
                if (opcion >= 1 && opcion <= menuItems.size()) {
                    seleccion = menuItems.get(opcion - 1);
                    estado = Estado.CONFIRMAR_SELECCION;
                    cb.onResponder("🍽️ Has elegido: " + seleccion.nombre +
                            " (Q" + seleccion.precio + "). ¿Confirmas? (sí/no)", false, null);
                } else {
                    cb.onResponder("❓ Opción no válida. Escribe 1, 2 o 3.", false, null);
                }
                break;

            case CONFIRMAR_SELECCION:
                if (msg.contains("si") || msg.contains("sí") || msg.contains("confirmo")) {
                    itemsSeleccionados.add(seleccion);
                    total += seleccion.precio;
                    cb.onResponder("✔️ Agregado: " + seleccion.nombre + " (Q" + seleccion.precio + ").", false, null);
                    estado = Estado.ESPERA_OTRO_COMBO;
                    cb.onResponder("¿Deseas agregar otro combo? (sí/no)", false, null);
                } else {
                    estado = Estado.ESPERA_SELECCION;
                    cb.onResponder("🔄 Está bien, escribe el número del platillo nuevamente.", false, null);
                }
                break;

            case ESPERA_OTRO_COMBO:
                if (msg.contains("si") || msg.contains("sí")) {
                    estado = Estado.MOSTRAR_MENU;
                    db.collection("usuario_menu")
                            .document(vendorId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    cb.onResponder("😔 Lo siento, no encontré tu menú.", false, null);
                                    estado = Estado.ESPERA_COMPRA;
                                    return;
                                }
                                menuItems.clear();
                                String imgB64 = doc.getString("imagenBase64");
                                menuItems.add(new MenuItem(doc.getString("menu1"), doc.getDouble("precio1"), imgB64));
                                menuItems.add(new MenuItem(doc.getString("menu2"), doc.getDouble("precio2"), imgB64));
                                menuItems.add(new MenuItem(doc.getString("menu3"), doc.getDouble("precio3"), imgB64));

                                cb.onResponder("[IMAGEN_BASE64] " + imgB64, false, null);

                                String texto = String.format(
                                        Locale.getDefault(),
                                        "📋 Nuestro menú del día (nuevamente):\n" +
                                                "1) %s — Q%.0f\n" +
                                                "2) %s — Q%.0f\n" +
                                                "3) %s — Q%.0f\n\n" +
                                                "Escribe el número del siguiente platillo.",
                                        doc.getString("menu1"), doc.getDouble("precio1"),
                                        doc.getString("menu2"), doc.getDouble("precio2"),
                                        doc.getString("menu3"), doc.getDouble("precio3")
                                );
                                cb.onResponder(texto, false, null);
                                estado = Estado.ESPERA_SELECCION;
                            })
                            .addOnFailureListener(e ->
                                    cb.onResponder("❌ Error cargando menú, inténtalo de nuevo.", false, null)
                            );
                } else {
                    estado = Estado.PEDIR_NOMBRE;
                    cb.onResponder("✅ Muy bien. ¿Cuál es tu nombre completo?", false, null);
                }
                break;

            case PEDIR_NOMBRE:
                pedidoEnProceso.put("nombreCliente", mensajeUsuario.trim());
                estado = Estado.PEDIR_DIRECCION;
                cb.onResponder("🏠 ¿Cuál es tu dirección de entrega?", false, null);
                break;

            case PEDIR_DIRECCION:
                pedidoEnProceso.put("direccion", mensajeUsuario.trim());
                estado = Estado.PEDIR_PAGO;
                cb.onResponder("💳 ¿Cómo prefieres pagar? (efectivo o tarjeta)", false, null);
                break;

            case PEDIR_PAGO:
                if (msg.contains("efectivo") || msg.contains("tarjeta")) {
                    pedidoEnProceso.put("pago", msg.contains("efectivo") ? "efectivo" : "tarjeta");
                    estado = Estado.PEDIR_OBSERVACION;
                    cb.onResponder("✍️ Si deseas agregar alguna observación al pedido, escríbela ahora. De lo contrario, escribe \"no\".", false, null);
                } else {
                    cb.onResponder("⚠️ Por favor escribe \"efectivo\" o \"tarjeta\" para continuar.", false, null);
                }
                break;

            case PEDIR_OBSERVACION:
                if (!msg.equalsIgnoreCase("no")) {
                    pedidoEnProceso.put("observacion", mensajeUsuario.trim());
                } else {
                    pedidoEnProceso.put("observacion", "");
                }
                estado = Estado.FINALIZADO;

                List<String> nombres = new ArrayList<>();
                for (MenuItem item : itemsSeleccionados) {
                    nombres.add(item.nombre);
                }
                pedidoEnProceso.put("producto", String.join(", ", nombres));
                pedidoEnProceso.put("precio", total);

                Map<String,Object> d = new HashMap<>(pedidoEnProceso);
                d.put("clienteId", clienteDocId);
                d.put("fecha", new Date());
                db.collection("cliente_pedido")
                        .add(d)
                        .addOnSuccessListener(ref ->
                                cb.onResponder(
                                        "🎉 ¡Listo! Tu pedido (" +
                                                pedidoEnProceso.get("producto") +
                                                ") ha sido registrado correctamente.\n" +
                                                "💲 Total: Q" + total + "\n\n" +
                                                "¡Gracias por tu compra! 🍔🍟",
                                        true, pedidoEnProceso
                                )
                        )
                        .addOnFailureListener(e ->
                                cb.onResponder("❌ Error guardando el pedido. Intenta de nuevo.", false, null)
                        );
                break;

            case FINALIZADO:
                cb.onResponder("🙏 Gracias por tu compra. ¡Que disfrutes tu comida! 🍽️", true, pedidoEnProceso);
                pedidoEnProceso.clear();
                itemsSeleccionados.clear();
                menuItems.clear();
                total = 0.0;
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
