package com.agh.aos.model;

import com.agh.aos.util.VectorUtil;
import io.vavr.collection.List;
import org.jscience.geography.coordinates.XYZ;
import org.jscience.mathematics.number.Float64;
import org.jscience.mathematics.vector.Float64Vector;
import org.jscience.physics.amount.Amount;

import javax.measure.VectorMeasure;
import javax.measure.quantity.*;

import static javax.measure.unit.SI.*;


public class AstronomicalObject {
    /**
     * MARS, SATURN etc. types for view (GUI)
     */
    public enum AstronomicObjectType {
        STAR, SUN,
        PLANET, EARTH, MERCURY, VENUS, MARS, JUPITER, SATURN, URANUS, NEPTUNE, PLUTO,
        SATELLITE, MOON
    }

    private AstronomicObjectType type;
    private String name;

    private Amount<Length> radius;
    private Amount<Mass> mass;

    private XYZ position;

    private VectorMeasure<Velocity> velocity;
    private VectorMeasure<Acceleration> acceleration;

    private double stepSize;

    public AstronomicalObject(AstronomicObjectType type, String name, Amount<Length> radius, Amount<Mass> mass, XYZ position, VectorMeasure<Velocity> velocity) {
        this.type = type;
        this.name = name;
        this.radius = radius;
        this.mass = mass;
        this.position = position;
        this.stepSize = 1.0;
        setVelocity(velocity);
        this.acceleration = VectorMeasure.valueOf(0.0, 0.0, 0.0, METERS_PER_SQUARE_SECOND);
    }

    public VectorMeasure<Acceleration> computeAccelerationSum(AstronomicalObjectSystem astronomicalObjectSystem) {
        Float64Vector f64acceleration = astronomicalObjectSystem.getAstronomicObjectList()
                .filter(ao -> !ao.equals(this))
                .map(ao -> computeAcceleration(this, ao, astronomicalObjectSystem.getGravitationalConstant()))
                .foldLeft(
                        Float64Vector
                                .valueOf(VectorMeasure.valueOf(0.0, 0.0, 0.0, METERS_PER_SQUARE_SECOND)
                                        .getValue()),
                        (a, v) -> a.plus(Float64Vector.valueOf(v.getValue()))
                );

        this.acceleration = VectorMeasure.valueOf(
                List.range(0, f64acceleration.getDimension())
                        .map(f64acceleration::getValue).toJavaStream()
                        .mapToDouble(v -> v).toArray(),
                METERS_PER_SQUARE_SECOND
        );
        return this.acceleration;
    }

    public VectorMeasure<Acceleration> computeAcceleration(AstronomicalObject off, AstronomicalObject by, Amount<?> gravitationalConstant) {


        Float64Vector deltaPosition = by.getPosition().toVector(METER).minus(off.getPosition().toVector(METER));

        double distanceValue = VectorUtil.length(deltaPosition);

        Float64Vector norm = Float64Vector.valueOf(
                List.range(0, deltaPosition.getDimension())
                        .map(i -> deltaPosition.getValue(i) / distanceValue)
                        .map(Float64::valueOf).toJavaList()
        );


        Amount<Length> distance = Amount.valueOf(distanceValue, METER);


        double value = gravitationalConstant
                .times(Math.pow(stepSize, 2))
                .times(by.getMass())
                .divide(distance.times(distance))
                .to(METERS_PER_SQUARE_SECOND)
                .doubleValue(METERS_PER_SQUARE_SECOND);

        Float64Vector f64Result = norm.times(value);

//        System.out.println(off.name
//                + " -> " + by.getName()
//                + " v: " + value
//                + " | " + f64Result
//                + " gc* " + gravitationalConstant.times(Math.pow(stepSize, 2)).toString()
//                + " m " + by.getMass().toString()
//                + " d*d " + distance.times(distance)
//        );

        return VectorUtil.float64VectorToVectorMeasure(f64Result, METERS_PER_SQUARE_SECOND);
    }

    public VectorMeasure<Velocity> updateVelocity() {
        Float64Vector v64 = Float64Vector.valueOf(this.velocity.getValue());
        Float64Vector a64 = Float64Vector.valueOf(this.acceleration.getValue());
        this.velocity = VectorUtil.float64VectorToVectorMeasure(v64.plus(a64), METERS_PER_SECOND);
        return this.velocity;
    }


    public void move() {
        this.position = XYZ.valueOf(
                position.toVector(METER).plus(Float64Vector.valueOf(this.velocity.getValue())),
                METER
        );
    }

    public AstronomicObjectType getType() {
        return type;
    }

    public void setType(AstronomicObjectType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Amount<Length> getRadius() {
        return radius;
    }

    public void setRadius(Amount<Length> radius) {
        this.radius = radius;
    }

    public Amount<Mass> getMass() {
        return mass;
    }

    public void setMass(Amount<Mass> mass) {
        this.mass = mass;
    }

    public XYZ getPosition() {
        return position;
    }

    public void setPosition(XYZ position) {
        this.position = position;
    }

    public VectorMeasure<Velocity> getVelocity() {
        return velocity;
    }

    public void setVelocity(VectorMeasure<Velocity> velocity) {
        this.velocity = VectorUtil.float64VectorToVectorMeasure(Float64Vector.valueOf(velocity.getValue()).times(stepSize), METERS_PER_SECOND);
    }

    public VectorMeasure<Acceleration> getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(VectorMeasure<Acceleration> acceleration) {
        this.acceleration = acceleration;
    }

    public double getStepSize() {
        return stepSize;
    }

    public void setStepSize(double stepSize) {
        this.velocity = VectorUtil.float64VectorToVectorMeasure(Float64Vector.valueOf(velocity.getValue()).times(1 / this.stepSize), METERS_PER_SECOND);
        this.stepSize = stepSize;
        setVelocity(this.velocity);
    }
}
