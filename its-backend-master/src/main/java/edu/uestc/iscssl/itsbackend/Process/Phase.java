package edu.uestc.iscssl.itsbackend.Process;

import com.google.gson.annotations.Expose;

public class Phase {
    @Expose
    private int phaseId;
    @Expose
    private double xPosition;
    @Expose
    private double yPosition;
    @Expose
    private int color;

    public Phase(int phaseId, double xPosition, double yPosition, int color) {
        this.phaseId = phaseId;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.color = color;
    }

    public int getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(int phaseId) {
        this.phaseId = phaseId;
    }

    public double getxPosition() {
        return xPosition;
    }

    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }

    public double getyPosition() {
        return yPosition;
    }

    public void setyPosition(double yPosition) {
        this.yPosition = yPosition;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
