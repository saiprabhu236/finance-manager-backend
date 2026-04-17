package com.sai.finance.finance_manager.portfolio.dto;

public class SectorAllocationDto {

    private String sector;
    private double percent;

    public SectorAllocationDto(String sector, double percent) {
        this.sector = sector;
        this.percent = percent;
    }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }
}
