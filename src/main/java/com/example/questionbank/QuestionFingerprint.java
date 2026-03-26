package com.example.questionbank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class QuestionFingerprint {
	private QuestionFingerprint() {
	}

	public static String compute(
			String questionText,
			String optionA,
			String optionB,
			String optionC,
			String optionD,
			String correctOption
	) {
		String normalizedQuestion = normalize(questionText);

		String correctKey = correctOption == null ? "" : correctOption.trim().toUpperCase(Locale.ROOT);
		String correctText = switch (correctKey) {
			case "A" -> optionA;
			case "B" -> optionB;
			case "C" -> optionC;
			case "D" -> optionD;
			default -> null;
		};
		String normalizedCorrectText = normalize(correctText);

		List<String> optionBucket = new ArrayList<>(4);
		optionBucket.add(normalize(optionA));
		optionBucket.add(normalize(optionB));
		optionBucket.add(normalize(optionC));
		optionBucket.add(normalize(optionD));
		optionBucket.sort(Comparator.naturalOrder());

		String canonical = normalizedQuestion + "|" + normalizedCorrectText + "|" + String.join("|", optionBucket);
		return sha256Hex(canonical);
	}

	public static String compute(Question question) {
		return compute(
				question.getQuestionText(),
				question.getOptionA(),
				question.getOptionB(),
				question.getOptionC(),
				question.getOptionD(),
				question.getCorrectOption()
		);
	}

	private static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.trim().toLowerCase(Locale.ROOT);
		s = s.replaceAll("\\s+", " ");
		s = s.replaceAll("[^a-z0-9 ]", "");
		return s;
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}

