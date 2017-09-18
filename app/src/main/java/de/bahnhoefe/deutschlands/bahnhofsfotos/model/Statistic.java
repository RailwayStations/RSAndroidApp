package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class Statistic {
    private final int total;
    private final int withPhoto;
    private final int withoutPhoto;
    private final int photographers;

    public Statistic(final int total, final int withPhoto, final int withoutPhoto, final int photographers) {
        this.total = total;
        this.withPhoto = withPhoto;
        this.withoutPhoto = withoutPhoto;
        this.photographers = photographers;
    }

    public int getTotal() {
        return total;
    }

    public int getWithPhoto() {
        return withPhoto;
    }

    public int getWithoutPhoto() {
        return withoutPhoto;
    }

    public int getPhotographers() {
        return photographers;
    }

    @Override
    public String toString() {
        return "Statistic{" +
                "total=" + total +
                ", withPhoto=" + withPhoto +
                ", withoutPhoto=" + withoutPhoto +
                ", photographers=" + photographers +
                '}';
    }

}
