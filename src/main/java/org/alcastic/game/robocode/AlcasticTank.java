package org.alcastic.game.robocode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import robocode.*;
import java.awt.Color;

public class AlcasticTank extends AdvancedRobot {

    //This class stores information about enemy
    @Getter
    @Setter
    private class Enemy {
        public String name; // Enemy Nick
        public double distance; // Distance until enemy
        public double energy; // Energy enemy
        public double speed; // Travel speed
        public long sighting; // Sighting date
        public double bearingRadians; // Orientation
        public double headingRadians; //cannon orientation
        public Point position = new Point(0, 0); // Enemy position on the field

        Enemy(int d) {
            this.distance = d;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private class Point {
        double x;
        double y;
    }

    static final double RANDOMNESS = 0.1;
    Enemy enemy;
    int direction;
    double potency;

    public void run() {
        setColors(new Color(0, 128, 255), Color.black, Color.red); //Tank's color
        enemy = new Enemy(Integer.MAX_VALUE);

        // Logistic variables
        Point aux = new Point(0, 0);
        direction = 1;
        potency = 0;

        // Movements of the radar, gun and robot are independent.
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        turnRadarRightRadians(2 * Math.PI);

        while (true) {
            // Scanning the environment
            setTurnRadarLeftRadians(2 * Math.PI);
            aux.setX(getX());
            aux.setY(getY());

            // Target the closest enemy detected
            target(aux, getFutureEnemyPosition(enemy));
            getPotency();

            //shooting
            fire(potency);

            // move
            drive();

            // Executing the actions
            execute();
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        System.out.print("\nHit by enemy bullet");
    }

    // When enemy is dead, increase the enemy distance to set the enemy in the next environment scanner.
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().compareTo(enemy.getName()) == 0) {
            enemy.setDistance(Integer.MAX_VALUE);
        }
    }

    void target(Point p1, Point p2) {
        double angleR = 0;
        double a = p2.getX() - p1.getX();
        double b = p2.getY() - p1.getY();
        double c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));

        // Cannon angle in radians if enemy is in first quadrant of the Cartesian plane (relative to current position).
        if (a > 0 && b > 0) {
            angleR = Math.asin(a / c);
        }
        // Cannon angle in radians if enemy is in second quadrant of the Cartesian plane (relative to current position).
        else if (a < 0 && b > 0) {
            angleR = 2.0 * Math.PI - Math.asin(-a / c);
        }
        // Cannon angle in radians if enemy is in third quadrant of the Cartesian plane (relative to current position).
        else if (a < 0 && b < 0) {
            angleR = Math.PI + Math.asin(-a / c);
        }
        // Cannon angle in radians if enemy is in fourth quadrant of the Cartesian plane (relative to current position).
        // if (a > 0 && b < 0)
        else{
            angleR = Math.PI - Math.asin(a / c);
        }

        // Current cannon angle.
        angleR = getGunHeadingRadians() - angleR;

        // getting angleR such that:  - Math.PI <= angleR <= Math.PI
        if (angleR > Math.PI) {
            angleR -= 2 * Math.PI;
        }
        if (angleR < -Math.PI) {
            angleR += 2 * Math.PI;
        }
        // Turns the cannon to the right if angleR < 0 or to the left if angleR > 0
        setTurnGunLeftRadians(angleR);
    }

    void drive() {
        double distance = enemy.getDistance();
        if (Math.random() < RANDOMNESS) {
            direction = -direction;
        }
        if (distance > 120) {
            setTurnRightRadians(enemy.getBearingRadians() + (Math.PI / 2));
        } else {
            setTurnRightRadians(enemy.getBearingRadians() + (Math.PI / 5));
        }
        setAhead(direction * 30);
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        direction *= -1;
        setAhead(direction * 30);
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
		// Detect the nearest enemy
        if ((e.getDistance() < enemy.getDistance()) || (enemy.getName().compareTo(e.getName()) == 0)) {
            // Updating enemy information
            enemy.setName(e.getName());
            enemy.setDistance(e.getDistance());
            enemy.setEnergy(e.getEnergy());
            enemy.setSpeed(e.getVelocity());
            enemy.setSighting(getTime());
            enemy.setBearingRadians(e.getBearingRadians());
            enemy.setHeadingRadians(e.getHeadingRadians());

            double absbearing_rad = (getHeadingRadians() + e.getBearingRadians()) % (2 * Math.PI);
            Point aux = new Point(
                    (getX() + Math.sin(absbearing_rad) * e.getDistance()),
                    (getY() + Math.cos(absbearing_rad) * e.getDistance())
            );
            enemy.setPosition(aux);
        }
    }

    public void getPotency() {
        if (enemy.getDistance() < 150) {
            this.potency = 3.0;
        } else if (enemy.getDistance() <= 200) {
            this.potency = 2.0;
        } else {
            this.potency = 500 / enemy.getDistance();
        }
        // The enemy power down as: max{ 4 * potency, 4 * pot + 2 * (potency - 1)}
        // Minimum damage = 4 * potency, maximum potency should be: (enemy energy)/ 4.
        if (potency > enemy.getEnergy()) {
            this.potency = enemy.getEnergy() / 4;
        }
    }

    public Point getFutureEnemyPosition(Enemy enemy) {
        // Bullet speed between: 11 and 19.7, depends on energy.
        // Speed = 20- 3 * power
        long deltaT = getTime() + (long) (enemy.getDistance() / (20 - 3 * potency));
        deltaT = deltaT - enemy.getSighting();
        double desplazamiento = enemy.getSpeed() * deltaT;
        double x = enemy.getPosition().getX() + desplazamiento * Math.sin(enemy.getHeadingRadians());
        double y = enemy.getPosition().getY() + desplazamiento * Math.cos(enemy.getHeadingRadians());
        return new Point(x, y);
    }

}