package model;

public enum SecurityQuestion {
    FAVORITE_COLOR(1, "What is your favorite color?"),
    FIRST_SCHOOL(2, "What was the name of your first school?"),
    FAVORITE_FOOD(3, "What is your favorite food?");

    private final int number;
    private final String text;

    SecurityQuestion(int number, String text) {
        this.number = number;
        this.text = text;
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public static SecurityQuestion fromNumber(int number) {
        for (SecurityQuestion question : values()) {
            if (question.number == number) {
                return question;
            }
        }
        return null;
    }

    public static String formattedList() {
        StringBuilder builder = new StringBuilder();
        for (SecurityQuestion question : values()) {
            builder.append(question.number)
                .append(") ")
                .append(question.text)
                .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }
}
