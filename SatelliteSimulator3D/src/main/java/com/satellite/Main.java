package com.satellite;

import com.satellite.dao.DatabaseConnection;
import com.satellite.dao.PlanetDAO;
import com.satellite.dao.SatelliteDAO;
import com.satellite.model.Planet;
import com.satellite.model.Satellite;
import com.satellite.physics.OrbitalSatellite;
import com.satellite.routing.LineOfSight;
import com.satellite.routing.RoutingEngine;
import com.satellite.routing.RoutingEngine.RoutingResult;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.geometry.Point3D;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Application {

    private static final double SCALE = 1.0 / 100.0;
    private static final double SAT_R_NORMAL = 1.5;
    private static final double SAT_R_RELAY  = 2.5;
    private static final double SUBSCENE_W = 900;
    private static final double SUBSCENE_H = 720;

    private final PlanetDAO planetDAO = new PlanetDAO();
    private final SatelliteDAO satelliteDAO = new SatelliteDAO();

    private Planet currentPlanet;
    private final Group worldGroup = new Group();
    private final Group satellitesGroup = new Group();
    private final Group laserGroup = new Group();
    private Sphere planetSphere;
    private PerspectiveCamera camera;

    private final Map<Integer, SatNode> nodes = new HashMap<>();
    private final ObservableList<Satellite> listData = FXCollections.observableArrayList();

    private final Rotate rotateX = new Rotate(-20, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    private double anchorX, anchorY, anchorAngleX, anchorAngleY;

    private AnimationTimer animator;
    private boolean playing = true;
    private double timeScale = 1000.0;
    private long lastNanos = 0;

    private ComboBox<Planet> planetCombo;
    private ComboBox<Satellite> fromCombo, toCombo;
    private Label statusLabel;
    private Label routeResultLabel;
    private RoutingResult currentRoute = null;
    private int routeSrcId = -1;
    private int routeDstId = -1;
    private boolean wasRouteIntact = true;

    @Override
    public void start(Stage stage) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            new Alert(AlertType.ERROR,
                "JavaFX 3D không được hỗ trợ (cần GPU hardware acceleration).").showAndWait();
            return;
        }
        if (DatabaseConnection.get() == null) {
            new Alert(AlertType.ERROR,
                "Không kết nối được SQL Server. Kiểm tra DB.").showAndWait();
            return;
        }

        BorderPane root = new BorderPane();
        root.setCenter(build3DScene());
        root.setRight(buildControlPanel());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1280, 780);
        stage.setScene(scene);
        stage.setTitle("Satellite Simulator 3D — Phase 1+2");
        stage.setOnCloseRequest(e -> {
            if (animator != null) animator.stop();
            DatabaseConnection.close();
        });
        stage.setMaximized(true);
        stage.show();

        loadPlanetsIntoUI();
        startAnimation();
    }

    private StackPane build3DScene() {
        worldGroup.getTransforms().addAll(rotateX, rotateY);
        worldGroup.getChildren().addAll(satellitesGroup, laserGroup);

        camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(8000);
        camera.setTranslateZ(-900);

        AmbientLight ambient = new AmbientLight(Color.rgb(70, 70, 80));
        PointLight sun = new PointLight(Color.WHITE);
        sun.setTranslateX(-800);
        sun.setTranslateY(-300);
        sun.setTranslateZ(-600);

        Group root3D = new Group(worldGroup, ambient, sun);

        SubScene sub = new SubScene(root3D, SUBSCENE_W, SUBSCENE_H, true, SceneAntialiasing.BALANCED);
        sub.setCamera(camera);
        sub.setFill(Color.web("#02030a"));

        sub.setOnMousePressed(this::onMousePressed);
        sub.setOnMouseDragged(this::onMouseDragged);
        sub.setOnScroll(this::onScroll);

        StackPane container = new StackPane(sub);
        container.setStyle("-fx-background-color: #02030a;");
        sub.widthProperty().bind(container.widthProperty());
        sub.heightProperty().bind(container.heightProperty());
        return container;
    }

    private ScrollPane buildControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12));
        panel.setPrefWidth(360);
        panel.setStyle("-fx-background-color: #1f2230;");

        Label planetLbl = section("Hành tinh");
        planetCombo = new ComboBox<>();
        planetCombo.setMaxWidth(Double.MAX_VALUE);
        planetCombo.setConverter(new StringConverter<>() {
            public String toString(Planet p) { return p == null ? "" : p.getName(); }
            public Planet fromString(String s) { return null; }
        });
        planetCombo.setItems(FXCollections.observableArrayList());
        planetCombo.setOnAction(e -> {
            Planet sel = planetCombo.getValue();
            if (sel != null && (currentPlanet == null || sel.getId() != currentPlanet.getId())) {
                setCurrentPlanet(sel);
            }
        });

        Label animLbl = section("Animation");
        Button playBtn = new Button("⏸ Pause");
        playBtn.setMaxWidth(Double.MAX_VALUE);
        playBtn.setOnAction(e -> {
            playing = !playing;
            playBtn.setText(playing ? "⏸ Pause" : "▶ Play");
        });
        Label scaleLbl = new Label("TIME_SCALE: 1000×");
        scaleLbl.setStyle("-fx-text-fill: #aab;");
        Slider scaleSlider = new Slider(1, 50000, 1000);
        scaleSlider.valueProperty().addListener((obs, ov, nv) -> {
            timeScale = nv.doubleValue();
            scaleLbl.setText(String.format("TIME_SCALE: %.0f×", timeScale));
        });

        Label listLbl = section("Danh sách vệ tinh");
        Label listCountLbl = new Label("Chưa tải");
        listCountLbl.setStyle("-fx-text-fill: #aab; -fx-font-size: 11px;");

        ListView<Satellite> listView = new ListView<>(listData);
        listView.setPrefHeight(180);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Satellite s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(String.format("%s%s  (alt %.0f km)",
                    s.isRelay() ? "📡 " : "🛰 ", s.getName(), s.getAltitudeKm()));
                setStyle("-fx-text-fill: black;");
            }
        });
        listData.addListener((javafx.collections.ListChangeListener<Satellite>) c -> {
            int n = listData.size();
            if (n == 0)
                listCountLbl.setText("⚠ Không có vệ tinh — chạy sql/seed_satellites.sql");
            else
                listCountLbl.setText(n + " vệ tinh");
        });

        Button delBtn = new Button("🗑 Xoá vệ tinh đã chọn");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        delBtn.setOnAction(e -> {
            Satellite sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) deleteSatellite(sel);
        });
        Button refreshBtn = new Button("↺ Làm mới danh sách");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> {
            if (currentPlanet != null) setCurrentPlanet(currentPlanet);
        });

        Label addLbl = section("Thêm vệ tinh mới");
        TextField nameF = new TextField(); nameF.setPromptText("Tên (vd: ISS-2)");
        TextField latF  = new TextField(); latF.setPromptText("Vĩ độ (-90 ÷ 90)");
        TextField lonF  = new TextField(); lonF.setPromptText("Kinh độ (-180 ÷ 180)");
        TextField altF  = new TextField(); altF.setPromptText("Độ cao (km)");
        CheckBox  relayCb = new CheckBox("Vệ tinh liên lạc (relay)");
        relayCb.setStyle("-fx-text-fill: #ddd;");

        GridPane form = new GridPane();
        form.setHgap(6); form.setVgap(6);
        form.addRow(0, lbl("Tên:"),    nameF);
        form.addRow(1, lbl("Lat:"),    latF);
        form.addRow(2, lbl("Lon:"),    lonF);
        form.addRow(3, lbl("Alt km:"), altF);

        Button addBtn = new Button("➕ Thêm vào DB");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            try {
                String name = nameF.getText().trim();
                if (name.isEmpty()) { warn("Tên không được trống"); return; }
                Satellite s = new Satellite(0, currentPlanet.getId(), name,
                    Double.parseDouble(latF.getText().trim()),
                    Double.parseDouble(lonF.getText().trim()),
                    Double.parseDouble(altF.getText().trim()),
                    relayCb.isSelected());
                try {
                    satelliteDAO.insert(s);
                    refreshAddedSatellites();
                } catch (RuntimeException ex) {
                    warn("Lỗi thêm vệ tinh:\n" + ex.getMessage());
                    return;
                }
                nameF.clear(); latF.clear(); lonF.clear(); altF.clear(); relayCb.setSelected(false);
            } catch (NumberFormatException ex) {
                warn("Lat / Lon / Alt phải là số.");
            }
        });

        // === Auto-generate section ===
        Label autoLbl = section("Sinh vệ tinh tự động");
        TextField autoNField = new TextField("20");
        autoNField.setPromptText("Số vệ tinh N");
        CheckBox autoRelayCb = new CheckBox("Là relay (tham gia định tuyến)");
        autoRelayCb.setSelected(true);
        autoRelayCb.setStyle("-fx-text-fill: #ddd;");
        Label autoHintLbl = new Label("Phân bố đều toàn hành tinh (Fibonacci sphere), độ cao 300–800 km ngẫu nhiên");
        autoHintLbl.setStyle("-fx-text-fill: #aab; -fx-font-size: 11px;");
        autoHintLbl.setWrapText(true);

        HBox autoRow = new HBox(8, lbl("Số N:"), autoNField);
        autoRow.setAlignment(Pos.CENTER_LEFT);

        Button autoGenBtn = new Button("⚡ Sinh N vệ tinh (xoá AUTO cũ)");
        autoGenBtn.setMaxWidth(Double.MAX_VALUE);
        autoGenBtn.setOnAction(e -> {
            try {
                int n = Integer.parseInt(autoNField.getText().trim());
                if (n < 2 || n > 500) { warn("N phải trong khoảng 2 – 500"); return; }
                autoGenSatellites(n, autoRelayCb.isSelected());
            } catch (NumberFormatException ex) {
                warn("N phải là số nguyên hợp lệ.");
            }
        });

        // === Routing section (Thành viên C) ===
        Label routeLbl = section("Định tuyến (Dijkstra)");

        fromCombo = new ComboBox<>(listData);
        fromCombo.setMaxWidth(Double.MAX_VALUE);
        fromCombo.setPromptText("Từ vệ tinh...");
        fromCombo.setConverter(satConverter());

        toCombo = new ComboBox<>(listData);
        toCombo.setMaxWidth(Double.MAX_VALUE);
        toCombo.setPromptText("Đến vệ tinh...");
        toCombo.setConverter(satConverter());

        Button routeBtn = new Button("\uD83D\uDD0D Tìm đường");
        routeBtn.setMaxWidth(Double.MAX_VALUE);
        routeBtn.setOnAction(e -> findRoute());

        Button clearRouteBtn = new Button("\u274C Xoá đường");
        clearRouteBtn.setMaxWidth(Double.MAX_VALUE);
        clearRouteBtn.setOnAction(e -> {
            currentRoute = null;
            laserGroup.getChildren().clear();
            routeResultLabel.setText("");
        });

        routeResultLabel = new Label("");
        routeResultLabel.setWrapText(true);
        routeResultLabel.setStyle("-fx-text-fill: #8fa;");

        panel.getChildren().addAll(
            planetLbl, planetCombo,
            new Separator(),
            animLbl, playBtn, scaleLbl, scaleSlider,
            new Separator(),
            listLbl, listCountLbl, listView, delBtn, refreshBtn,
            new Separator(),
            addLbl, form, relayCb, addBtn,
            new Separator(),
            autoLbl, autoRow, autoRelayCb, autoHintLbl, autoGenBtn,
            new Separator(),
            routeLbl,
            lbl("Từ:"), fromCombo,
            lbl("Đến:"), toCombo,
            routeBtn, clearRouteBtn, routeResultLabel
        );
        VBox.setVgrow(listView, Priority.SOMETIMES);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefWidth(380);
        scroll.setStyle("-fx-background: #1f2230; -fx-background-color: #1f2230;");
        return scroll;
    }

    private HBox buildStatusBar() {
        statusLabel = new Label("Sẵn sàng");
        statusLabel.setStyle("-fx-text-fill: #8fa;");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.setStyle("-fx-background-color: #0f1118;");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void loadPlanetsIntoUI() {
        List<Planet> planets = planetDAO.findAll();
        planetCombo.getItems().setAll(planets);
        if (!planets.isEmpty()) {
            planetCombo.getSelectionModel().selectFirst();
            setCurrentPlanet(planets.get(0));
        }
    }

    private void setCurrentPlanet(Planet planet) {
        this.currentPlanet = planet;
        if (planetSphere != null) worldGroup.getChildren().remove(planetSphere);
        satellitesGroup.getChildren().clear();
        laserGroup.getChildren().clear();
        currentRoute = null;
        nodes.clear();
        listData.clear();

        double radiusScene = planet.getRadiusKm() * SCALE;
        planetSphere = new Sphere(radiusScene);
        planetSphere.setMaterial(buildPlanetMaterial(planet));
        worldGroup.getChildren().add(0, planetSphere);

        try {
            List<Satellite> sats = satelliteDAO.findByPlanet(planet.getId());
            for (Satellite s : sats) addSatelliteNode(s);
        } catch (RuntimeException ex) {
            new Alert(AlertType.ERROR,
                "Lỗi tải vệ tinh từ DB:\n" + ex.getMessage()
                + "\n\nHãy chạy sql/seed_satellites.sql trong SSMS.").showAndWait();
        }

        camera.setTranslateZ(-Math.max(800, radiusScene * 10));
        updateStatus();
    }

    private PhongMaterial buildPlanetMaterial(Planet planet) {
        PhongMaterial mat = new PhongMaterial();
        Image tex = loadTexture("/textures/" + planet.getTextureFile());
        if (tex != null) {
            mat.setDiffuseMap(tex);
        } else {
            mat.setDiffuseColor(planet.getName().equalsIgnoreCase("Mars")
                ? Color.web("#c1440e") : Color.web("#2b6cb0"));
        }
        mat.setSpecularColor(Color.web("#3a3a3a"));
        return mat;
    }

    private Image loadTexture(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            return (is != null) ? new Image(is) : null;
        } catch (Exception e) { return null; }
    }

    private void addSatelliteNode(Satellite s) {
        SatNode sn = new SatNode(s, currentPlanet, SCALE);
        nodes.put(s.getId(), sn);
        satellitesGroup.getChildren().add(sn.sphere);
        listData.add(s);
    }

    private void refreshAddedSatellites() {
        List<Satellite> all = satelliteDAO.findByPlanet(currentPlanet.getId());
        for (Satellite s : all) {
            if (!nodes.containsKey(s.getId())) addSatelliteNode(s);
        }
        updateStatus();
    }

    private void deleteSatellite(Satellite s) {
        try {
            satelliteDAO.delete(s.getId());
        } catch (RuntimeException ex) {
            warn("Lỗi xoá vệ tinh:\n" + ex.getMessage());
            return;
        }
        SatNode sn = nodes.remove(s.getId());
        if (sn != null) {
            satellitesGroup.getChildren().remove(sn.sphere);
            listData.remove(s);
        }
        updateStatus();
    }

    private void updateStatus() {
        statusLabel.setText(String.format("Hành tinh: %s  |  Vệ tinh: %d  |  Kéo chuột để xoay, lăn để zoom",
            currentPlanet == null ? "—" : currentPlanet.getName(), nodes.size()));
    }

    private void startAnimation() {
        animator = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNanos == 0) { lastNanos = now; return; }
                double dt = (now - lastNanos) / 1e9;
                lastNanos = now;
                if (!playing) return;
                for (SatNode sn : nodes.values()) sn.update(dt, timeScale);
                if (currentRoute != null && currentRoute.found()) updateLasers();
            }
        };
        animator.start();
    }

    private void onMousePressed(MouseEvent e) {
        anchorX = e.getSceneX();
        anchorY = e.getSceneY();
        anchorAngleX = rotateX.getAngle();
        anchorAngleY = rotateY.getAngle();
    }

    private void onMouseDragged(MouseEvent e) {
        rotateY.setAngle(anchorAngleY + (e.getSceneX() - anchorX) * 0.4);
        rotateX.setAngle(anchorAngleX - (e.getSceneY() - anchorY) * 0.4);
    }

    private void onScroll(ScrollEvent e) {
        double z = camera.getTranslateZ();
        camera.setTranslateZ(Math.min(-30, z + e.getDeltaY() * 2));
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #cde; -fx-font-weight: bold; -fx-font-size: 13px;");
        return l;
    }

    private static Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #ccd;");
        return l;
    }

    private void warn(String msg) {
        new Alert(AlertType.WARNING, msg).showAndWait();
    }

    // ==================== Routing (Thành viên C) ====================

    private StringConverter<Satellite> satConverter() {
        return new StringConverter<>() {
            public String toString(Satellite s) {
                return s == null ? "" : (s.isRelay() ? "\uD83D\uDCE1 " : "\uD83D\uDEF0 ") + s.getName();
            }
            public Satellite fromString(String str) { return null; }
        };
    }

    private void findRoute() {
        Satellite from = fromCombo.getValue();
        Satellite to   = toCombo.getValue();
        if (from == null || to == null) { warn("Chọn vệ tinh nguồn và đích"); return; }
        if (from.getId() == to.getId()) { warn("Nguồn và đích phải khác nhau"); return; }

        routeSrcId = from.getId();
        routeDstId = to.getId();
        wasRouteIntact = true;

        double R = currentPlanet.getRadiusKm() * SCALE;
        currentRoute = RoutingEngine.findRoute(new ArrayList<>(listData), routeSrcId, routeDstId, R);
        routeResultLabel.setStyle("-fx-text-fill: #8fa;");
        routeResultLabel.setText(currentRoute.summary());
        updateLasers();
    }

    private void updateLasers() {
        laserGroup.getChildren().clear();
        if (currentRoute == null || !currentRoute.found()) return;
        double R = currentPlanet.getRadiusKm() * SCALE;
        List<Satellite> path = currentRoute.path();
        boolean routeIntact = true;
        for (int i = 0; i < path.size() - 1; i++) {
            Satellite a = findLiveSatellite(path.get(i).getId());
            Satellite b = findLiveSatellite(path.get(i + 1).getId());
            if (a != null && b != null) {
                if (!LineOfSight.hasLOS(a, b, R)) routeIntact = false;
            }
        }
        if (!routeIntact && wasRouteIntact) {
            wasRouteIntact = false;
            tryReroute(R);
            return;
        }
        wasRouteIntact = routeIntact;
        for (int i = 0; i < path.size() - 1; i++) {
            Satellite a = findLiveSatellite(path.get(i).getId());
            Satellite b = findLiveSatellite(path.get(i + 1).getId());
            if (a != null && b != null)
                laserGroup.getChildren().add(makeLaser(a, b, true));
        }
    }

    private void tryReroute(double planetR) {
        if (routeSrcId < 0 || routeDstId < 0) return;
        RoutingResult newRoute = RoutingEngine.findRoute(
            new ArrayList<>(listData), routeSrcId, routeDstId, planetR);
        if (newRoute.found()) {
            currentRoute = newRoute;
            wasRouteIntact = true;
            routeResultLabel.setStyle("-fx-text-fill: #8fa;");
            routeResultLabel.setText("↺ Tự động định tuyến lại: " + newRoute.summary());
        } else {
            currentRoute = null;
            laserGroup.getChildren().clear();
            routeResultLabel.setStyle("-fx-text-fill: #f88;");
            routeResultLabel.setText("✗ Không tìm thấy đường — tất cả tuyến bị che khuất.");
        }
    }

    private void autoGenSatellites(int n, boolean isRelay) {
        if (currentPlanet == null) return;
        try {
            int removed = satelliteDAO.deleteAutoGenerated(currentPlanet.getId());
            if (removed > 0) {
                nodes.entrySet().removeIf(entry -> {
                    Satellite s = entry.getValue().orbital.getSatellite();
                    if (s.getName().startsWith("AUTO-")) {
                        satellitesGroup.getChildren().remove(entry.getValue().sphere);
                        listData.remove(s);
                        return true;
                    }
                    return false;
                });
            }
        } catch (RuntimeException ex) {
            warn("Lỗi xoá AUTO cũ: " + ex.getMessage());
            return;
        }

        // Fibonacci sphere — phân bố đều N điểm trên mặt cầu
        double golden = (1 + Math.sqrt(5)) / 2.0;
        java.util.Random rng = new java.util.Random();
        double altMin = 300, altMax = 800;

        for (int i = 0; i < n; i++) {
            // lat: từ -90° đến +90° phân bố đều theo sin
            double lat = Math.toDegrees(Math.asin(1.0 - 2.0 * (i + 0.5) / n));
            // lon: xoay theo tỉ lệ vàng để tránh xếp hàng
            double lon = (360.0 * i / golden) % 360.0;
            if (lon > 180) lon -= 360.0;
            double alt = altMin + rng.nextDouble() * (altMax - altMin);

            Satellite s = new Satellite(0, currentPlanet.getId(),
                "AUTO-" + (i + 1), lat, lon, alt, isRelay);
            try {
                satelliteDAO.insert(s);
            } catch (RuntimeException ex) {
                warn("Lỗi thêm AUTO-" + (i + 1) + ": " + ex.getMessage());
                break;
            }
        }
        refreshAddedSatellites();
        currentRoute = null;
        laserGroup.getChildren().clear();
        routeResultLabel.setText("");
    }

    private Satellite findLiveSatellite(int id) {
        SatNode sn = nodes.get(id);
        return sn != null ? sn.orbital.getSatellite() : null;
    }

    private Cylinder makeLaser(Satellite a, Satellite b, boolean losOk) {
        // JavaFX Y-axis flip: dùng -getY()
        double ax = a.getX(), ay = -a.getY(), az = a.getZ();
        double bx = b.getX(), by = -b.getY(), bz = b.getZ();

        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Cylinder cyl = new Cylinder(0.4, dist);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(losOk ? Color.LIME : Color.RED);
        cyl.setMaterial(mat);

        // Đặt giữa 2 điểm
        cyl.setTranslateX((ax + bx) / 2);
        cyl.setTranslateY((ay + by) / 2);
        cyl.setTranslateZ((az + bz) / 2);

        // Xoay cylinder (mặc định dọc Y) để nối 2 điểm
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D direction = new Point3D(dx, dy, dz).normalize();
        Point3D axis = yAxis.crossProduct(direction);
        double angle = Math.toDegrees(Math.acos(
            Math.max(-1, Math.min(1, yAxis.dotProduct(direction)))));

        if (axis.magnitude() > 1e-10) {
            cyl.getTransforms().add(new Rotate(angle, axis));
        }
        return cyl;
    }

    private static class SatNode {
        final OrbitalSatellite orbital;
        final Sphere sphere;

        SatNode(Satellite s, Planet p, double scale) {
            this.orbital = new OrbitalSatellite(s, p, scale);
            double r = s.isRelay() ? SAT_R_RELAY : SAT_R_NORMAL;
            this.sphere = new Sphere(r);
            PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(s.isRelay() ? Color.GOLD : Color.LIGHTCYAN);
            mat.setSpecularColor(Color.WHITE);
            sphere.setMaterial(mat);
            apply();
        }

        void update(double dt, double timeScale) {
            orbital.update(dt, timeScale);
            apply();
        }

        private void apply() {
            Satellite s = orbital.getSatellite();
            sphere.setTranslateX(s.getX());
            sphere.setTranslateY(-s.getY()); // JavaFX Y axis points down
            sphere.setTranslateZ(s.getZ());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
