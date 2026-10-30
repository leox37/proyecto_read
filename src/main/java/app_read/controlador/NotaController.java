package app_read.controlador;

import app_read.modelo.Nota;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class NotaController {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Pane barra_color;
    @FXML
    private TextArea area_fragmento;
    @FXML
    private Label label_tipo;
    @FXML
    private Label label_fecha;
    @FXML
    private TextArea area_declaracion1;
    @FXML
    private javafx.scene.image.ImageView boton_borrar;

    public void configurar(Nota nota, Runnable alBorrar) {
        Objects.requireNonNull(nota);
        area_fragmento.setText(nota.getFragmentoOrigen());
        area_declaracion1.setText(nota.getContenidoTexto());
        area_declaracion1.setEditable(false);
        label_tipo.setText(nota.getCategoriaTema());
        label_fecha.setText(nota.getFechaCreacion().format(FECHA));
        barra_color.setStyle("-fx-background-color: " + nota.getColorResaltado() + ";");
        boton_borrar.setOnMouseClicked(event -> alBorrar.run());
    }
}
