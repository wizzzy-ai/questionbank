package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.Bookmark;
import com.example.questionbank.BookmarkRepository;
import com.example.questionbank.Question;
import com.example.questionbank.QuestionRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class BookmarksController {
	private final AuthService authService;
	private final BookmarkRepository bookmarkRepository;
	private final QuestionRepository questionRepository;

	public BookmarksController(AuthService authService, BookmarkRepository bookmarkRepository, QuestionRepository questionRepository) {
		this.authService = authService;
		this.bookmarkRepository = bookmarkRepository;
		this.questionRepository = questionRepository;
	}

	@GetMapping("/bookmarks")
	public String bookmarks(HttpSession session, Model model) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		Student student = studentId == null ? null : authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}
		var bookmarks = bookmarkRepository.findAllByStudentIdFetchQuestionOrderByCreatedAtDesc(studentId);
		model.addAttribute("student", student);
		model.addAttribute("bookmarks", bookmarks);
		model.addAttribute("bookmarkCount", bookmarks.size());
		model.addAttribute("categoryCount", bookmarks.stream()
				.map(Bookmark::getQuestion)
				.map(Question::getCategory)
				.filter(category -> category != null && !category.isBlank())
				.collect(Collectors.toSet())
				.size());
		return "bookmarks";
	}

	@PostMapping("/bookmarks/remove")
	public String remove(@RequestParam("questionId") Long questionId, HttpSession session) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		Student student = studentId == null ? null : authService.findById(studentId).orElse(null);
		if (student == null) {
			session.invalidate();
			return "redirect:/login";
		}
		if (questionId != null) {
			bookmarkRepository.findByStudentIdAndQuestionId(studentId, questionId)
					.ifPresent(bookmarkRepository::delete);
		}
		return "redirect:/bookmarks";
	}

	@PostMapping("/bookmarks/toggle")
	@ResponseBody
	public Map<String, Object> toggle(@RequestParam("questionId") Long questionId, HttpSession session) {
		Long studentId = (Long) session.getAttribute(SessionKeys.STUDENT_ID);
		Student student = studentId == null ? null : authService.findById(studentId).orElse(null);
		if (student == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
		}
		if (questionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId is required");
		}

		Question question = questionRepository.findById(questionId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

		return bookmarkRepository.findByStudentIdAndQuestionId(studentId, questionId)
				.map(existing -> {
					bookmarkRepository.delete(existing);
					return Map.<String, Object>of("bookmarked", false);
				})
				.orElseGet(() -> {
					bookmarkRepository.save(new Bookmark(student, question));
					return Map.<String, Object>of("bookmarked", true);
				});
	}
}
