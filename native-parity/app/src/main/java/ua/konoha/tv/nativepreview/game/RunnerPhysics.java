package ua.konoha.tv.nativepreview.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RunnerPhysics {
    public static final double W = 1280.0;
    public static final double H = 540.0;
    public static final double GROUND = 440.0;

    public static class HeroDef {
        public final String name;
        public final String title;
        public final double width;
        public final double height;

        public HeroDef(String name, String title, double width, double height) {
            this.name = name;
            this.title = title;
            this.width = width;
            this.height = height;
        }
    }

    public static final HeroDef HERO_NARUTO = new HeroDef("Наруто", "Ніколи не здаватися.", 48.0, 84.0);
    public static final HeroDef HERO_GAMABUNTA = new HeroDef("Гамабунта", "Один стрибок — ціла пригода.", 80.0, 65.0);

    public static class Rect {
        public double x, y, w, h;
        public Rect(double x, double y, double w, double h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    public static boolean overlap(Rect a, Rect b) {
        return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
    }

    public static class Obstacle {
        public int id;
        public String kind; // "log", "stake", "shuriken"
        public double x, y, w, h;
        public boolean passed;

        public Obstacle(int id, String kind, double x, double y, double w, double h) {
            this.id = id;
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.passed = false;
        }
    }

    public static class Pose {
        public int frame;
        public String mode; // "jump", "slide", "run", "idle"
        public double hop;

        public Pose(int frame, String mode, double hop) {
            this.frame = frame;
            this.mode = mode;
            this.hop = hop;
        }
    }

    public static Pose poseFor(RunnerPhysics run) {
        double rate = ("gamabunta".equals(run.hero) ? 6.4 : 11.0) * Math.sqrt(run.speed / 360.0);
        if (run.y > 1.0) {
            return new Pose(4, "jump", 0.0);
        }
        if (run.duck > 0.0) {
            double progress = Math.max(0.0, Math.min(1.0, 1.0 - run.duck / 0.72));
            int frame = Math.min(3, (int) Math.floor(progress * 4.0));
            return new Pose(frame, "slide", 0.0);
        }
        if ("running".equals(run.status) || "paused".equals(run.status)) {
            int frame = (int) (Math.floor(run.elapsed * rate) % 4);
            if (frame < 0) frame = (frame % 4 + 4) % 4;
            double hop = 0.0;
            if ("gamabunta".equals(run.hero)) {
                double[] hops = new double[]{0.0, -3.0, -9.0, 0.0};
                hop = hops[frame];
            }
            return new Pose(frame, "run", hop);
        }
        return new Pose(5, "idle", 0.0);
    }

    public interface RandomSource {
        double nextDouble();
    }

    private final RandomSource randomSource;
    public String hero = "naruto";
    public String status = "ready"; // "ready", "running", "paused", "over"
    public double distance = 0.0;
    public double elapsed = 0.0;
    public double y = 0.0;
    public double vy = 0.0;
    public double duck = 0.0;
    public double speed = 360.0;
    public List<Obstacle> obstacles = new ArrayList<>();
    public double next = 1.8;
    public int sequence = 0;
    public int cleared = 0;

    public RunnerPhysics() {
        this("naruto", new Random()::nextDouble);
    }

    public RunnerPhysics(String hero) {
        this(hero, new Random()::nextDouble);
    }

    public RunnerPhysics(String hero, RandomSource randomSource) {
        this.randomSource = randomSource;
        setHero(hero);
        reset();
    }

    public void setHero(String hero) {
        if ("gamabunta".equals(hero)) {
            this.hero = "gamabunta";
        } else {
            this.hero = "naruto";
        }
    }

    public HeroDef getHeroDef() {
        return "gamabunta".equals(hero) ? HERO_GAMABUNTA : HERO_NARUTO;
    }

    public void reset() {
        status = "ready";
        distance = 0.0;
        elapsed = 0.0;
        y = 0.0;
        vy = 0.0;
        duck = 0.0;
        speed = 360.0;
        obstacles.clear();
        next = 1.8;
        sequence = 0;
        cleared = 0;
    }

    public void start() {
        reset();
        status = "running";
    }

    public boolean jump() {
        if (!"running".equals(status) || y > 0.5) return false;
        duck = 0.0;
        vy = -810.0;
        y = 0.01;
        return true;
    }

    public void crouch() {
        if (!"running".equals(status)) return;
        if (y > 0.0) {
            vy = Math.max(400.0, vy);
        } else {
            duck = 0.72;
        }
    }

    public Rect box() {
        HeroDef h = getHeroDef();
        double height = duck > 0.0 ? 28.0 : h.height;
        return new Rect(185.0, GROUND - y - height, h.width, height);
    }

    public void spawn() {
        double pick = randomSource.nextDouble();
        String kind;
        if (distance > 900.0 && pick > 0.67) {
            kind = "shuriken";
        } else if (pick < 0.5) {
            kind = "log";
        } else {
            kind = "stake";
        }

        double w = "shuriken".equals(kind) ? 42.0 : "log".equals(kind) ? 64.0 : 40.0;
        double h = "shuriken".equals(kind) ? 34.0 : "log".equals(kind) ? 48.0 : 74.0;
        double yPos = "shuriken".equals(kind) ? GROUND - 96.0 : GROUND - h;

        obstacles.add(new Obstacle(++sequence, kind, W + 70.0, yPos, w, h));
    }

    public void step(double dt) {
        if (!"running".equals(status)) return;
        double remaining = Math.min(Math.max(dt, 0.0), 0.25);
        while (remaining > 0.0 && "running".equals(status)) {
            double s = Math.min(remaining, 1.0 / 120.0);
            remaining -= s;
            elapsed += s;
            speed = Math.min(760.0, 360.0 + distance * 0.012);
            distance += speed * s;
            duck = Math.max(0.0, duck - s);

            if (y > 0.0 || vy < 0.0) {
                vy += 2100.0 * s;
                y -= vy * s;
                if (y < 0.0) {
                    y = 0.0;
                    vy = 0.0;
                }
            }

            next -= s;
            if (next <= 0.0) {
                spawn();
                next = 1.28 + randomSource.nextDouble() * 0.72;
            }

            Rect player = box();
            for (Obstacle o : obstacles) {
                o.x -= speed * s;
                if (!o.passed && o.x + o.w < player.x) {
                    o.passed = true;
                    cleared++;
                }
                Rect oBox = new Rect(o.x + 5.0, o.y + 5.0, o.w - 10.0, o.h - 7.0);
                if (overlap(player, oBox)) {
                    status = "over";
                    break;
                }
            }

            Iterator<Obstacle> it = obstacles.iterator();
            while (it.hasNext()) {
                Obstacle o = it.next();
                if (o.x + o.w <= -60.0) {
                    it.remove();
                }
            }
        }
    }

    public int getScore() {
        return (int) Math.floor(distance / 10.0);
    }
}
