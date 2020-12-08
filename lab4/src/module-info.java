module Lab4 {
    requires javafx.fxml;
    requires javafx.controls;
    requires protobuf.java;

    opens forms.main;
    opens forms.create;
    opens forms.join;
    opens textures;
    opens game;
}