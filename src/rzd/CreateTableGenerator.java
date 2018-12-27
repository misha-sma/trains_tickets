package rzd;

public class CreateTableGenerator {
	public static void main(String[] args) {
		StringBuilder builder = new StringBuilder();
		builder.append(
				"CREATE TABLE IF NOT EXISTS seats (id_seat bigserial PRIMARY KEY, id_carriage bigint, seat_number int");
		for (int i = 1; i <= 150; ++i) {
			builder.append(", stage_" + i + " bool DEFAULT 'f'");
		}
		builder.append(");");
		System.out.println("sql=" + builder.toString());
	}
}
