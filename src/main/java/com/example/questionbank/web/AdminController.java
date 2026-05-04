package com.example.questionbank.web;

import com.example.questionbank.AuthService;
import com.example.questionbank.AdminRoleService;
import com.example.questionbank.Category;
import com.example.questionbank.CategoryRepository;
import com.example.questionbank.Question;
import com.example.questionbank.QuestionDifficultyBand;
import com.example.questionbank.QuestionRepository;
import com.example.questionbank.QuestionStatus;
import com.example.questionbank.QuizResult;
import com.example.questionbank.QuizResultRepository;
import com.example.questionbank.ReportIssue;
import com.example.questionbank.ReportIssueRepository;
import com.example.questionbank.SessionKeys;
import com.example.questionbank.Student;
import com.example.questionbank.StudentRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {
	private static final Logger log = LoggerFactory.getLogger(AdminController.class);
	
	private final AuthService authService;
	private final AdminRoleService adminRoleService;
	private final CategoryRepository categoryRepository;
	private final QuestionRepository questionRepository;
	private final StudentRepository studentRepository;
	private final QuizResultRepository quizResultRepository;
	private final ReportIssueRepository reportIssueRepository;

	public AdminController(
			AuthService authService,
			AdminRoleService adminRoleService,
			CategoryRepository categoryRepository,
			QuestionRepository questionRepository,
			StudentRepository studentRepository,
			QuizResultRepository quizResultRepository,
			ReportIssueRepository reportIssueRepository
	) {
		this.authService = authService;
		this.adminRoleService = adminRoleService;
		this.categoryRepository = categoryRepository;
		this.questionRepository = questionRepository;
		this.studentRepository = studentRepository;
		this.quizResultRepository = quizResultRepository;
		this.reportIssueRepository = reportIssueRepository;
	}

	@GetMapping("")
	public String dashboard(HttpSession session, Model model) {
		log.info("Admin dashboard accessed");
		addAdminContext(session, model, "dashboard");

		List<Question> questions = loadQuestions();
		List<Student> students = loadStudents();
		List<QuizResult> recentResults = quizResultRepository.findTop8ByOrderBySubmittedAtDesc();

		model.addAttribute("totalQuestions", questions.size());
		model.addAttribute("totalUsers", students.size());
		model.addAttribute("quizzesTaken", quizResultRepository.count());
		model.addAttribute("activeToday", quizResultRepository.countDistinctStudentsActiveSince(Instant.now().minus(1, ChronoUnit.DAYS)));
		model.addAttribute("recentActivity", buildRecentActivity(students, recentResults, questions));
		return "admin_dashboard";
	}

	@GetMapping("/questions")
	public String questionAdmin(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "saved", required = false) Boolean saved,
			HttpSession session,
			Model model
	) {
		addAdminContext(session, model, "questions");

		List<Question> questions = loadQuestions();
		List<String> categories = categoryRepository.findAllByOrderByNameAsc().stream().map(Category::getName).toList();

		model.addAttribute("questions", questions);
		model.addAttribute("categories", categories);
		model.addAttribute("query", query == null ? "" : query);
		model.addAttribute("selectedCategory", category == null ? "" : category);
		model.addAttribute("saved", saved != null && saved);
		model.addAttribute("totalQuestions", questions.size());
		model.addAttribute("activeQuestions", questions.stream().filter(question -> question.getStatus() == QuestionStatus.ACTIVE).count());
		model.addAttribute("draftQuestions", questions.stream().filter(question -> question.getStatus() == QuestionStatus.DRAFT).count());
		model.addAttribute("categoryCount", categories.size());
		return "admin_questions";
	}

	@GetMapping("/questions/new")
	public String newQuestion(
			@RequestParam(value = "category", required = false) String category,
			HttpSession session,
			Model model
	) {
		addAdminContext(session, model, "questions");
		if (!model.containsAttribute("questionForm")) {
			AdminQuestionForm form = new AdminQuestionForm();
			form.setCorrectOption("A");
			form.setCategoryId(resolveCategory(category == null || category.isBlank() ? "General" : category.trim()).getId());
			form.setStatus(QuestionStatus.ACTIVE);
			model.addAttribute("questionForm", form);
		}
		addQuestionFormMeta(model, "Create Question", false);
		return "admin_question_form";
	}

	@GetMapping("/questions/{id}")
	public String viewQuestion(@PathVariable("id") Long id, HttpSession session, Model model) {
		addAdminContext(session, model, "questions");
		Question question = questionRepository.findById(id).orElseThrow();
		model.addAttribute("questionForm", toForm(question));
		addQuestionFormMeta(model, "Question Details", true);
		return "admin_question_form";
	}

	@GetMapping("/questions/{id}/edit")
	public String editQuestion(@PathVariable("id") Long id, HttpSession session, Model model) {
		addAdminContext(session, model, "questions");
		Question question = questionRepository.findById(id).orElseThrow();
		if (!model.containsAttribute("questionForm")) {
			model.addAttribute("questionForm", toForm(question));
		}
		addQuestionFormMeta(model, "Edit Question", false);
		return "admin_question_form";
	}

	@PostMapping("/questions/save")
	public String saveQuestion(@Valid @ModelAttribute("questionForm") AdminQuestionForm form, BindingResult bindingResult, HttpSession session, Model model) {
		addAdminContext(session, model, "questions");
		if (bindingResult.hasErrors()) {
			addQuestionFormMeta(model, form.getId() == null ? "Create Question" : "Edit Question", false);
			return "admin_question_form";
		}

		Question question = form.getId() == null
				? new Question()
				: questionRepository.findById(form.getId()).orElse(new Question());
		applyForm(question, form);
		questionRepository.save(question);
		return "redirect:/admin/questions?saved=true";
	}

	@PostMapping("/questions/delete")
	public String deleteQuestion(@RequestParam("id") Long id) {
		questionRepository.deleteById(id);
		return "redirect:/admin/questions?saved=true";
	}

	@PostMapping("/questions/{id}/duplicate")
	public String duplicateQuestion(@PathVariable("id") Long id) {
		Question source = questionRepository.findById(id).orElseThrow();
		Question duplicate = new Question(
				source.getQuestionText(),
				source.getOptionA(),
				source.getOptionB(),
				source.getOptionC(),
				source.getOptionD(),
				source.getCorrectOption(),
				source.getCategory()
		);
		duplicate.setCategoryEntity(resolveCategory(source.getCategory()));
		duplicate.setDifficultyBand(source.getDifficultyBand());
		duplicate.setStatus(QuestionStatus.DRAFT);
		questionRepository.save(duplicate);
		return "redirect:/admin/questions?saved=true";
	}

	@PostMapping("/questions/bulk")
	public String bulkQuestionAction(
			@RequestParam(value = "ids", required = false) List<Long> ids,
			@RequestParam("bulkAction") String bulkAction
	) {
		if (ids == null || ids.isEmpty()) {
			return "redirect:/admin/questions";
		}

		List<Question> questions = questionRepository.findAllById(ids);
		switch (bulkAction == null ? "" : bulkAction) {
			case "delete" -> questionRepository.deleteAll(questions);
			case "active" -> {
				questions.forEach(question -> question.setStatus(QuestionStatus.ACTIVE));
				questionRepository.saveAll(questions);
			}
			case "draft" -> {
				questions.forEach(question -> question.setStatus(QuestionStatus.DRAFT));
				questionRepository.saveAll(questions);
			}
			default -> {
			}
		}
		return "redirect:/admin/questions?saved=true";
	}

	@PostMapping("/questions/import")
	public String importQuestions(@RequestParam("payload") String payload) {
		List<Question> imported = new ArrayList<>();
		if (payload != null) {
			for (String line : payload.split("\\R")) {
				String trimmed = line.trim();
				if (trimmed.isBlank()) {
					continue;
				}
				String[] parts = trimmed.split("\\|");
				if (parts.length < 8) {
					continue;
				}
				String category = parts[0].trim();
				String difficulty = parts[1].trim();
				String questionText = parts[2].trim();
				String optionA = parts[3].trim();
				String optionB = parts[4].trim();
				String optionC = parts[5].trim();
				String optionD = parts[6].trim();
				String correctOption = parts[7].trim().toUpperCase(Locale.ROOT);

				boolean exists = questionRepository.findFirstByQuestionTextIgnoreCaseAndCategoryIgnoreCase(questionText, category).isPresent();
				if (exists) {
					continue;
				}

				Question question = new Question(questionText, optionA, optionB, optionC, optionD, correctOption, category);
				question.setCategoryEntity(resolveCategory(category));
				try {
					question.setDifficultyBand(QuestionDifficultyBand.valueOf(difficulty.toUpperCase(Locale.ROOT)));
				} catch (IllegalArgumentException ignored) {
					question.setDifficultyBand(QuestionDifficultyBand.MEDIUM);
				}
				question.setStatus(QuestionStatus.ACTIVE);
				imported.add(question);
			}
		}

		if (!imported.isEmpty()) {
			questionRepository.saveAll(imported);
		}
		return "redirect:/admin/questions?saved=true";
	}

	@GetMapping("/users")
	public String usersPage(
			@RequestParam(value = "saved", required = false) Boolean saved,
			@RequestParam(value = "message", required = false) String message,
			@RequestParam(value = "error", required = false) String error,
			HttpSession session,
			Model model
	) {
		addAdminContext(session, model, "users");
		model.addAttribute("users", loadStudents());
		model.addAttribute("saved", saved != null && saved);
		model.addAttribute("message", message);
		model.addAttribute("error", error);
		return "admin_users";
	}

	@GetMapping("/users/{id}")
	public String userDetails(
			@PathVariable("id") Long id,
			@RequestParam(value = "message", required = false) String message,
			@RequestParam(value = "error", required = false) String error,
			HttpSession session,
			Model model
	) {
		addAdminContext(session, model, "users");
		Student userRecord = studentRepository.findById(id).orElse(null);
		if (userRecord == null) {
			return "redirect:/admin/users?error=User+not+found";
		}
		model.addAttribute("userRecord", userRecord);
		model.addAttribute("recentResults", quizResultRepository.findTop20ByStudentIdOrderBySubmittedAtDesc(id).stream().limit(5).toList());
		model.addAttribute("message", message);
		model.addAttribute("error", error);
		
		// Computed properties to avoid complex ternary expressions in Thymeleaf
		String userRoleLabel = userRecord.getRole().name();
		String userRoleClass = userRecord.getRole().hasAdminAccess() ? "admin-pill--admin" : "admin-pill--user";
		String userStatusLabel = userRecord.isDeleted() ? "Deleted" : (userRecord.isBanned() ? "Banned" : "Active");
		String userStatusClass = userRecord.isDeleted() ? "admin-pill--draft" : (userRecord.isBanned() ? "admin-pill--banned" : "admin-pill--active");
		
		model.addAttribute("userRoleLabel", userRoleLabel);
		model.addAttribute("userRoleClass", userRoleClass);
		model.addAttribute("userStatusLabel", userStatusLabel);
		model.addAttribute("userStatusClass", userStatusClass);
		
		return "admin_user_detail";
	}

	@PostMapping("/users/{id}/toggle-ban")
	public String toggleBan(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		studentRepository.findById(id).ifPresent(student -> {
			if (current != null && Objects.equals(current.getId(), student.getId())) {
				return;
			}
			if (student.isDeleted() || student.isAdmin() || student.isBootstrapAdmin()) {
				return;
			}
			student.setBanned(!student.isBanned());
			studentRepository.save(student);
		});
		return "redirect:/admin/users?saved=true";
	}

	@PostMapping("/users/{id}/soft-delete")
	public String softDeleteUser(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		studentRepository.findById(id).ifPresent(student -> {
			if (current != null && Objects.equals(current.getId(), student.getId())) {
				return;
			}
			if (student.isAdmin() || student.isBootstrapAdmin()) {
				return;
			}
			student.setDeleted(true);
			student.setDeletedAt(Instant.now());
			student.setBanned(true);
			student.setLeaderboardVisible(false);
			student.setVerificationToken(null);
			student.setVerificationTokenExpiresAt(null);
			student.setPasswordResetToken(null);
			student.setPasswordResetTokenExpiresAt(null);
			studentRepository.save(student);
		});
		return "redirect:/admin/users?saved=true";
	}

	@PostMapping("/users/{id}/restore")
	public String restoreUser(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		studentRepository.findById(id).ifPresent(student -> {
			if (current != null && Objects.equals(current.getId(), student.getId())) {
				return;
			}
			if (student.isBootstrapAdmin()) {
				return;
			}
			student.setDeleted(false);
			student.setDeletedAt(null);
			student.setBanned(false);
			studentRepository.save(student);
		});
		return "redirect:/admin/users?saved=true";
	}

	@PostMapping("/users/{id}/promote-admin")
	public String promoteUserToAdmin(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		if (current == null || !current.isSuperAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Only+the+super+admin+can+assign+admin+roles";
		}
		adminRoleService.promoteToAdmin(current, id);
		return "redirect:/admin/users/" + id + "?message=User+promoted+to+admin";
	}

	@PostMapping("/users/{id}/demote-admin")
	public String demoteAdminToUser(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		if (current == null || !current.isSuperAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Only+the+super+admin+can+remove+admin+roles";
		}
		adminRoleService.demoteToUser(current, id);
		return "redirect:/admin/users/" + id + "?message=Admin+role+removed";
	}

	@PostMapping("/users/{id}/transfer-super-admin")
	public String transferSuperAdmin(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		if (current == null || !current.isSuperAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Only+the+current+super+admin+can+transfer+ownership";
		}
		if (Objects.equals(current.getId(), id)) {
			return "redirect:/admin/users/" + id + "?error=Choose+another+user+to+receive+super+admin";
		}
		adminRoleService.transferSuperAdmin(current, id);
		session.invalidate();
		return "redirect:/login?info=Super+admin+role+transferred.+Please+sign+in+again.";
	}

	@PostMapping("/users/{id}/delete-bootstrap-admin")
	public String deleteBootstrapAdmin(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		if (current == null || !current.isSuperAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Only+the+super+admin+can+remove+the+bootstrap+admin";
		}
		adminRoleService.deleteBootstrapAdmin(current, id);
		return "redirect:/admin/users?message=Bootstrap+admin+deleted";
	}

	@PostMapping("/users/{id}/permanent-delete")
	public String permanentDeleteUser(@PathVariable("id") Long id, HttpSession session) {
		Student current = loadCurrentStudent(session).orElse(null);
		if (current == null || !current.isAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Only+admins+can+permanently+delete+users";
		}
		
		Optional<Student> studentOpt = studentRepository.findById(id);
		if (studentOpt.isEmpty()) {
			return "redirect:/admin/users?error=User+not+found";
		}
		
		Student student = studentOpt.get();
		
		// Prevent self-deletion
		if (Objects.equals(current.getId(), id)) {
			return "redirect:/admin/users/" + id + "?error=Cannot+delete+yourself";
		}
		
		// Prevent deletion of super admin
		if (student.isSuperAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Cannot+delete+super+admin";
		}
		
		// Prevent deletion of bootstrap admin
		if (student.isBootstrapAdmin()) {
			return "redirect:/admin/users/" + id + "?error=Cannot+delete+bootstrap+admin";
		}
		
		try {
			log.warn("Admin {} permanently deleting user: {} ({})", current.getEmail(), student.getFullName(), student.getEmail());
			
			// Delete all related data first (quiz results, etc.)
			quizResultRepository.deleteByStudentId(id);
			
			// Permanently delete the user
			studentRepository.deleteById(id);
			
			return "redirect:/admin/users?message=User+permanently+deleted";
		} catch (Exception e) {
			log.error("Failed to permanently delete user {}: {}", id, e.getMessage(), e);
			return "redirect:/admin/users/" + id + "?error=Failed+to+delete+user";
		}
	}

	@GetMapping("/categories")
	public String categoriesPage(
			@RequestParam(value = "saved", required = false) Boolean saved,
			HttpSession session,
			Model model
	) {
		addAdminContext(session, model, "categories");
		List<CategorySummary> categories = buildCategorySummaries();
		model.addAttribute("categories", categories);
		model.addAttribute("saved", saved != null && saved);
		return "admin_categories";
	}

	@GetMapping("/categories/new")
	public String newCategory(HttpSession session, Model model) {
		addAdminContext(session, model, "categories");
		if (!model.containsAttribute("categoryForm")) {
			model.addAttribute("categoryForm", new CategoryForm());
		}
		model.addAttribute("pageTitle", "Add Category");
		return "admin_category_form";
	}

	@GetMapping("/categories/{id}/edit")
	public String editCategory(@PathVariable("id") Long id, HttpSession session, Model model) {
		addAdminContext(session, model, "categories");
		Category category = categoryRepository.findById(id).orElseThrow();
		if (!model.containsAttribute("categoryForm")) {
			CategoryForm form = new CategoryForm();
			form.setId(category.getId());
			form.setName(category.getName());
			form.setDescription(category.getDescription());
			form.setColor(category.getColor());
			model.addAttribute("categoryForm", form);
		}
		model.addAttribute("pageTitle", "Edit Category");
		return "admin_category_form";
	}

	@PostMapping("/categories/save")
	public String saveCategory(@Valid @ModelAttribute("categoryForm") CategoryForm form, BindingResult bindingResult, HttpSession session, Model model) {
		addAdminContext(session, model, "categories");
		Category existingCategory = form.getId() == null ? null : categoryRepository.findById(form.getId()).orElse(null);
		String normalized = existingCategory != null && "General".equalsIgnoreCase(existingCategory.getName())
				? "General"
				: normalizeCategoryName(form.getName());
		categoryRepository.findByNameIgnoreCase(normalized)
				.filter(existing -> !Objects.equals(existing.getId(), form.getId()))
				.ifPresent(existing -> bindingResult.rejectValue("name", "category.duplicate", "Category already exists"));
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", form.getId() == null ? "Add Category" : "Edit Category");
			return "admin_category_form";
		}

		Category category = existingCategory == null ? new Category() : existingCategory;
		String previousName = category.getName();
		category.setName(normalized);
		category.setDescription(form.getDescription());
		category.setColor(form.getColor());
		category = categoryRepository.save(category);

		if (previousName != null && !previousName.equalsIgnoreCase(normalized)) {
			List<Question> linkedQuestions = questionRepository.findByCategoryEntityId(category.getId());
			for (Question question : linkedQuestions) {
				question.setCategoryEntity(category);
			}
			questionRepository.saveAll(linkedQuestions);
		}
		return "redirect:/admin/categories?saved=true";
	}

	@PostMapping("/categories/{id}/delete")
	public String deleteCategory(
			@PathVariable("id") Long id,
			@RequestParam(value = "moveToCategoryId", required = false) Long moveToCategoryId,
			@RequestParam(value = "deleteQuestions", required = false) Boolean deleteQuestions) {
		Category category = categoryRepository.findById(id).orElseThrow();
		if ("General".equalsIgnoreCase(category.getName())) {
			return "redirect:/admin/categories";
		}

		List<Question> linkedQuestions = questionRepository.findByCategoryEntityId(id);

		if (Boolean.TRUE.equals(deleteQuestions)) {
			questionRepository.deleteAll(linkedQuestions);
		} else {
			Category targetCategory;
			if (moveToCategoryId != null && !moveToCategoryId.equals(id)) {
				targetCategory = categoryRepository.findById(moveToCategoryId).orElseGet(() -> resolveCategory("General"));
			} else {
				targetCategory = resolveCategory("General");
			}
			for (Question question : linkedQuestions) {
				question.setCategoryEntity(targetCategory);
			}
			questionRepository.saveAll(linkedQuestions);
		}

		categoryRepository.delete(category);
		return "redirect:/admin/categories?saved=true";
	}

	@PostMapping("/categories/bulk-delete")
	public String bulkDeleteCategories(@RequestParam("ids") List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return "redirect:/admin/categories";
		}

		Category general = resolveCategory("General");

		for (Long id : ids) {
			Category category = categoryRepository.findById(id).orElse(null);
			if (category == null || "General".equalsIgnoreCase(category.getName())) {
				continue;
			}

			List<Question> linkedQuestions = questionRepository.findByCategoryEntityId(id);
			for (Question question : linkedQuestions) {
				question.setCategoryEntity(general);
			}
			questionRepository.saveAll(linkedQuestions);
			categoryRepository.delete(category);
		}

		return "redirect:/admin/categories?saved=true";
	}

	@PostMapping("/categories/merge")
	public String mergeCategories(
			@RequestParam("targetId") Long targetId,
			@RequestParam("sourceIds") List<Long> sourceIds) {
		if (sourceIds == null || sourceIds.isEmpty() || targetId == null) {
			return "redirect:/admin/categories";
		}

		Category target = categoryRepository.findById(targetId).orElse(null);
		if (target == null) {
			return "redirect:/admin/categories";
		}

		for (Long sourceId : sourceIds) {
			if (sourceId.equals(targetId)) {
				continue;
			}

			Category source = categoryRepository.findById(sourceId).orElse(null);
			if (source == null || "General".equalsIgnoreCase(source.getName())) {
				continue;
			}

			List<Question> linkedQuestions = questionRepository.findByCategoryEntityId(sourceId);
			for (Question question : linkedQuestions) {
				question.setCategoryEntity(target);
			}
			questionRepository.saveAll(linkedQuestions);
			categoryRepository.delete(source);
		}

		return "redirect:/admin/categories?saved=true";
	}

	@GetMapping("/categories/{id}/questions")
	@ResponseBody
	public CategoryQuestionsResponse categoryQuestions(@PathVariable("id") Long id) {
		Category category = categoryRepository.findById(id).orElseThrow();
		List<CategoryQuestionSummary> questions = questionRepository
				.findTop5ByCategoryEntityIdOrderByCreatedAtDescIdDesc(id)
				.stream()
				.map(question -> new CategoryQuestionSummary(
						question.getId(),
						question.getQuestionText(),
						question.getDifficultyBand().name(),
						question.getCreatedAt() == null ? null : question.getCreatedAt().toString()
				))
				.toList();
		return new CategoryQuestionsResponse(
				category.getId(),
				category.getName(),
				questionRepository.countByCategoryEntityId(id),
				questions
		);
	}

	@PostMapping("/categories/{categoryId}/questions/{questionId}/remove")
	@ResponseBody
	public java.util.Map<String, Object> removeQuestionFromCategory(
			@PathVariable("categoryId") Long categoryId,
			@PathVariable("questionId") Long questionId
	) {
		Question question = questionRepository.findById(questionId).orElseThrow();
		Category category = categoryRepository.findById(categoryId).orElseThrow();
		if (question.getCategoryEntity() == null || !Objects.equals(question.getCategoryEntity().getId(), categoryId)) {
			return java.util.Map.of("ok", false);
		}
		question.setCategoryEntity(resolveCategory("General"));
		questionRepository.save(question);
		long remaining = questionRepository.countByCategoryEntityId(categoryId);
		return java.util.Map.of(
				"ok", true,
				"removedQuestionId", questionId,
				"remainingCount", remaining,
				"categoryName", category.getName()
		);
	}

	@GetMapping("/reports")
	public String reportsPage(HttpSession session, Model model) {
		addAdminContext(session, model, "reports");
		return "admin_reports";
	}

	@GetMapping("/api/reports/summary")
	@ResponseBody
	public ReportSummaryResponse reportSummary(
			@RequestParam(value = "start", required = false) String start,
			@RequestParam(value = "end", required = false) String end
	) {
		ReportRange range = resolveReportRange(start, end);
		return new ReportSummaryResponse(buildMetricCards(range), range.startDate().toString(), range.endDate().toString());
	}

	@GetMapping("/api/reports/charts")
	@ResponseBody
	public ReportChartsResponse reportCharts(
			@RequestParam(value = "start", required = false) String start,
			@RequestParam(value = "end", required = false) String end
	) {
		ReportRange range = resolveReportRange(start, end);
		return new ReportChartsResponse(
				buildDailyMetricPoints(range, true),
				buildDailyMetricPoints(range, false)
		);
	}

	@GetMapping("/api/reports/categories")
	@ResponseBody
	public List<ReportCategoryBar> reportCategories(
			@RequestParam(value = "start", required = false) String start,
			@RequestParam(value = "end", required = false) String end
	) {
		return buildTopCategories(resolveReportRange(start, end));
	}

	@GetMapping("/api/reports/activity")
	@ResponseBody
	public List<ReportActivityItem> reportActivity(
			@RequestParam(value = "start", required = false) String start,
			@RequestParam(value = "end", required = false) String end,
			@RequestParam(value = "limit", required = false, defaultValue = "8") Integer limit
	) {
		return buildReportActivity(resolveReportRange(start, end), limit);
	}

	@GetMapping("/api/reports/export")
	@ResponseBody
	public String exportReport(
			@RequestParam(value = "start", required = false) String start,
			@RequestParam(value = "end", required = false) String end,
			@RequestParam(value = "format", required = false, defaultValue = "csv") String format
	) {
		ReportRange range = resolveReportRange(start, end);
		if ("csv".equalsIgnoreCase(format)) {
			return buildCsvExport(range);
		}
		return "PDF export not implemented";
	}

	@GetMapping("/notifications")
	public String notificationsPage(
			@RequestParam(value = "tab", required = false) String tab,
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "date", required = false) String date,
			HttpSession session,
			Model model
	) {
		addAdminContext(session, model, "notifications");
		model.addAttribute("notificationTab", tab == null || tab.isBlank() ? "all" : tab);
		model.addAttribute("notificationQuery", query == null ? "" : query);
		model.addAttribute("notificationDate", date == null ? "" : date);
		return "admin_notifications";
	}

	@GetMapping("/settings")
	public String settingsPage(HttpSession session, Model model) {
		Student current = loadCurrentStudent(session).orElse(null);
		if (current == null || !current.isSuperAdmin()) {
			return "redirect:/admin";
		}
		addAdminContext(session, model, "settings");
		model.addAttribute("siteName", "Quizora Admin");
		model.addAttribute("defaultTime", 15);
		model.addAttribute("maxQuestions", 20);
		return "admin_settings";
	}

	private void addQuestionFormMeta(Model model, String pageTitle, boolean readOnly) {
		model.addAttribute("difficultyOptions", QuestionDifficultyBand.values());
		model.addAttribute("statusOptions", QuestionStatus.values());
		model.addAttribute("categoryOptions", categoryRepository.findAllByOrderByNameAsc());
		model.addAttribute("pageTitle", pageTitle);
		model.addAttribute("readOnly", readOnly);
	}

	private void addAdminContext(HttpSession session, Model model, String activePage) {
		loadCurrentStudent(session).ifPresent(student -> {
			model.addAttribute("student", student);
			model.addAttribute("isSuperAdmin", student.isSuperAdmin());
		});
		model.addAttribute("activeAdminPage", activePage);
		model.addAttribute("adminNotifications", buildAdminNotifications());
	}

	private List<AdminNotification> buildAdminNotifications() {
		List<AdminNotification> notifications = new ArrayList<>();
		Instant now = Instant.now();

		studentRepository.findAll().stream()
				.filter(Student::isAdmin)
				.sorted(Comparator.comparing(Student::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.findFirst()
				.ifPresent(admin -> notifications.add(new AdminNotification(
						"admin-welcome",
						"system",
						"Admin workspace ready",
						admin.getFullName() + " can review activity, questions, and reports from one place.",
						"/admin/settings",
						"Review settings",
						admin.getCreatedAt() == null ? now.minus(3, ChronoUnit.HOURS) : admin.getCreatedAt(),
						formatRelativeTime(admin.getCreatedAt() == null ? now.minus(3, ChronoUnit.HOURS) : admin.getCreatedAt()),
						false
				)));

		loadStudents().stream()
				.filter(student -> !student.isAdmin())
				.limit(2)
				.forEach(student -> notifications.add(new AdminNotification(
						"user-" + student.getId(),
						"user",
						"New user registered",
						student.getFullName() + " joined the platform and is ready for review.",
						"/admin/users",
						"Go to user",
						student.getCreatedAt() == null ? now.minus(25, ChronoUnit.MINUTES) : student.getCreatedAt(),
						formatRelativeTime(student.getCreatedAt() == null ? now.minus(25, ChronoUnit.MINUTES) : student.getCreatedAt()),
						true
				)));

		quizResultRepository.findTop8ByOrderBySubmittedAtDesc().stream()
				.limit(2)
				.forEach(result -> notifications.add(new AdminNotification(
						"quiz-" + result.getId(),
						"quiz",
						"Quiz completed",
						result.getStudent().getFullName() + " completed a " + (result.getMode() == null ? "adaptive" : result.getMode().toLowerCase(Locale.ROOT)) + " quiz with " +
								Math.round(result.getPercentage()) + "% accuracy.",
						"/admin/reports",
						"View analytics",
						result.getSubmittedAt() == null ? now.minus(14, ChronoUnit.MINUTES) : result.getSubmittedAt(),
						formatRelativeTime(result.getSubmittedAt() == null ? now.minus(14, ChronoUnit.MINUTES) : result.getSubmittedAt()),
						true
				)));

		buildCategorySummaries().stream()
				.filter(summary -> summary.questionCount() == 0)
				.findFirst()
				.ifPresent(summary -> notifications.add(new AdminNotification(
						"report-empty-" + summary.id(),
						"report",
						"Empty category needs attention",
						summary.name() + " has no questions assigned right now.",
						"/admin/categories",
						"Resolve",
						summary.createdAt() == null ? now.minus(50, ChronoUnit.MINUTES) : summary.createdAt(),
						formatRelativeTime(summary.createdAt() == null ? now.minus(50, ChronoUnit.MINUTES) : summary.createdAt()),
						true
				)));

		if (notifications.size() < 6) {
			notifications.add(new AdminNotification(
					"system-sync",
					"system",
					"Daily sync complete",
					"Category counts and recent quiz activity are in sync across the admin workspace.",
					"/admin/reports",
					"View summary",
					now.minus(90, ChronoUnit.MINUTES),
					formatRelativeTime(now.minus(90, ChronoUnit.MINUTES)),
					false
			));
		}

		return notifications.stream()
				.sorted(Comparator.comparing(AdminNotification::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(8)
				.toList();
	}

	private String formatRelativeTime(Instant createdAt) {
		if (createdAt == null) {
			return "Just now";
		}

		long minutes = Math.max(1, ChronoUnit.MINUTES.between(createdAt, Instant.now()));
		if (minutes < 60) {
			return minutes + "m ago";
		}
		long hours = ChronoUnit.HOURS.between(createdAt, Instant.now());
		if (hours < 24) {
			return hours + "h ago";
		}
		long days = ChronoUnit.DAYS.between(createdAt, Instant.now());
		if (days < 7) {
			return days + "d ago";
		}
		return java.time.format.DateTimeFormatter.ofPattern("dd MMM")
				.withLocale(Locale.ENGLISH)
				.withZone(ZoneId.systemDefault())
				.format(createdAt);
	}

	private Optional<Student> loadCurrentStudent(HttpSession session) {
		Object rawStudentId = session.getAttribute(SessionKeys.STUDENT_ID);
		Long studentId = rawStudentId instanceof Number number ? number.longValue() : null;
		if (studentId == null) {
			return Optional.empty();
		}
		return authService.findById(studentId);
	}

	private List<Question> loadQuestions() {
		return questionRepository.findAll().stream()
				.sorted(Comparator
						.comparing(Question::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(Question::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	private List<Student> loadStudents() {
		return studentRepository.findAll().stream()
				.sorted(Comparator
						.comparing(Student::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(Student::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	private List<QuizResult> loadResults() {
		return quizResultRepository.findAll().stream()
				.sorted(Comparator
						.comparing(QuizResult::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(QuizResult::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	private ReportRange resolveReportRange(String start, String end) {
		ZoneId zone = ZoneId.of("Africa/Lagos");
		LocalDate today = LocalDate.now(zone);
		LocalDate endDate = parseDateOrDefault(end, today);
		LocalDate startDate = parseDateOrDefault(start, endDate.minusDays(29));
		if (startDate.isAfter(endDate)) {
			LocalDate swap = startDate;
			startDate = endDate;
			endDate = swap;
		}
		long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		LocalDate previousEndDate = startDate.minusDays(1);
		LocalDate previousStartDate = previousEndDate.minusDays(days - 1);
		return new ReportRange(
				startDate,
				endDate,
				startDate.atStartOfDay(zone).toInstant(),
				endDate.plusDays(1).atStartOfDay(zone).toInstant(),
				previousStartDate,
				previousEndDate,
				previousStartDate.atStartOfDay(zone).toInstant(),
				previousEndDate.plusDays(1).atStartOfDay(zone).toInstant()
		);
	}

	private LocalDate parseDateOrDefault(String value, LocalDate fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return LocalDate.parse(value.trim());
		} catch (DateTimeParseException ignored) {
			return fallback;
		}
	}

	private List<MetricCard> buildMetricCards(ReportRange range) {
		int currentUsers = (int) studentRepository.countAllUsersBetween(range.startInstant(), range.endExclusive());
		int previousUsers = (int) studentRepository.countAllUsersBetween(range.previousStartInstant(), range.previousEndExclusive());
		int currentQuestions = (int) questionRepository.countCreatedBetween(range.startInstant(), range.endExclusive());
		int previousQuestions = (int) questionRepository.countCreatedBetween(range.previousStartInstant(), range.previousEndExclusive());
		int currentQuizzes = (int) quizResultRepository.countSubmittedBetween(range.startInstant(), range.endExclusive());
		int previousQuizzes = (int) quizResultRepository.countSubmittedBetween(range.previousStartInstant(), range.previousEndExclusive());
		int currentActive = (int) studentRepository.countActiveBetween(
				range.endDate().atStartOfDay(ZoneId.of("Africa/Lagos")).toInstant(),
				range.endDate().plusDays(1).atStartOfDay(ZoneId.of("Africa/Lagos")).toInstant());
		int previousActive = (int) studentRepository.countActiveBetween(
				range.previousEndDate().atStartOfDay(ZoneId.of("Africa/Lagos")).toInstant(),
				range.previousEndDate().plusDays(1).atStartOfDay(ZoneId.of("Africa/Lagos")).toInstant());
		int currentIssues = (int) reportIssueRepository.countByStatus(ReportIssue.Status.OPEN);
		int previousIssues = (int) reportIssueRepository.countByStatusBetween(ReportIssue.Status.OPEN, range.previousStartInstant(), range.previousEndExclusive());

		return List.of(
				new MetricCard("users", "bi-people", "Total Users", currentUsers, trendPercent(currentUsers, previousUsers), currentUsers >= previousUsers, "vs previous period"),
				new MetricCard("questions", "bi-patch-question", "Total Questions", currentQuestions, trendPercent(currentQuestions, previousQuestions), currentQuestions >= previousQuestions, "questions created in range"),
				new MetricCard("quizzes", "bi-clipboard-check", "Quizzes Completed", currentQuizzes, trendPercent(currentQuizzes, previousQuizzes), currentQuizzes >= previousQuizzes, "submissions in range"),
				new MetricCard("active", "bi-lightning-charge", "Active Today", currentActive, trendPercent(currentActive, previousActive), currentActive >= previousActive, "distinct learners on latest day"),
				new MetricCard("issues", "bi-flag", "Reported Issues", currentIssues, trendPercent(currentIssues, previousIssues), currentIssues <= previousIssues, "alerts requiring admin action")
		);
	}

	private int trendPercent(int current, int previous) {
		if (previous == 0) {
			return current > 0 ? 100 : 0;
		}
		return (int) Math.round(((current - previous) * 100.0) / previous);
	}

	private List<DailyMetricPoint> buildDailyMetricPoints(ReportRange range, boolean userSeries) {
		List<DailyMetricPoint> points = new ArrayList<>();
		Map<LocalDate, Integer> valuesByDate = mapDailyCounts(userSeries
				? studentRepository.countRegisteredGroupedByDate(range.startInstant(), range.endExclusive())
				: quizResultRepository.countSubmittedGroupedByDate(range.startInstant(), range.endExclusive()));
		for (LocalDate day = range.startDate(); !day.isAfter(range.endDate()); day = day.plusDays(1)) {
			points.add(new DailyMetricPoint(day.toString(), valuesByDate.getOrDefault(day, 0)));
		}
		return points;
	}

	private Map<LocalDate, Integer> mapDailyCounts(List<Object[]> rows) {
		Map<LocalDate, Integer> map = new HashMap<>();
		for (Object[] row : rows) {
			LocalDate date = row[0] instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate() : null;
			Integer count = row[1] instanceof Number number ? number.intValue() : 0;
			if (date != null) {
				map.put(date, count);
			}
		}
		return map;
	}

	private List<ReportCategoryBar> buildTopCategories(ReportRange range) {
		Map<Long, String> categoryColors = buildCategorySummaries().stream()
				.collect(java.util.stream.Collectors.toMap(
						CategorySummary::id,
						summary -> summary.color() != null ? summary.color() : resolveCategoryColor(summary.name(), null)
				));

		List<ReportCategoryBar> categories = questionRepository.countCreatedByCategoryBetween(range.startInstant(), range.endExclusive())
				.stream()
				.map(row -> {
					Long categoryId = row[0] instanceof Number number ? number.longValue() : null;
					String categoryName = row[1] == null ? "General" : row[1].toString();
					long questionCount = row[2] instanceof Number number ? number.longValue() : 0L;
					return new ReportCategoryBar(
							categoryId,
							categoryName,
							questionCount,
							0,
							categoryColors.getOrDefault(categoryId, resolveCategoryColor(categoryName, null))
					);
				})
				.sorted(Comparator.comparing(ReportCategoryBar::questionCount).reversed().thenComparing(ReportCategoryBar::name))
				.limit(6)
				.toList();
		long max = Math.max(1, categories.stream().mapToLong(ReportCategoryBar::questionCount).max().orElse(1));
		return categories.stream()
				.map(category -> new ReportCategoryBar(
						category.id(),
						category.name(),
						category.questionCount(),
						(int) Math.round((category.questionCount() * 100.0) / max),
						category.color()
				))
				.toList();
	}

	private List<ReportActivityItem> buildReportActivity(ReportRange range, int limit) {
		List<ReportActivityItem> items = new ArrayList<>();
		loadStudents().stream()
				.filter(student -> !student.isAdmin() && inRange(student.getCreatedAt(), range.startInstant(), range.endExclusive()))
				.limit(limit)
				.forEach(student -> items.add(new ReportActivityItem(
						"user-" + student.getId(),
						"user",
						student.getFullName(),
						"registered a new account",
						"/admin/users/" + student.getId(),
						student.getCreatedAt(),
						formatRelativeTime(student.getCreatedAt())
				)));
		loadResults().stream()
				.filter(result -> inRange(result.getSubmittedAt(), range.startInstant(), range.endExclusive()))
				.limit(limit)
				.forEach(result -> items.add(new ReportActivityItem(
						"quiz-" + result.getId(),
						"quiz",
						result.getStudent().getFullName(),
						"completed a " + safeMode(result.getMode()) + " quiz with " + Math.round(result.getPercentage()) + "% accuracy",
						"/admin/reports",
						result.getSubmittedAt(),
						formatRelativeTime(result.getSubmittedAt())
				)));
		loadQuestions().stream()
				.filter(question -> inRange(question.getCreatedAt(), range.startInstant(), range.endExclusive()))
				.limit(limit)
				.forEach(question -> items.add(new ReportActivityItem(
						"question-" + question.getId(),
						"question",
						"Question Bank",
						"added a " + question.getDifficultyBand().name().toLowerCase(Locale.ROOT) + " question in " + question.getCategory(),
						"/admin/questions/" + question.getId(),
						question.getCreatedAt(),
						formatRelativeTime(question.getCreatedAt())
				)));
		return items.stream()
				.sorted(Comparator.comparing(ReportActivityItem::time, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(limit)
				.toList();
	}

	private String buildCsvExport(ReportRange range) {
		StringBuilder csv = new StringBuilder();
		csv.append("Date,New Users,Quiz Completions,Questions Created\n");
		
		Map<LocalDate, Integer> usersByDate = mapDailyCounts(studentRepository.countRegisteredGroupedByDate(range.startInstant(), range.endExclusive()));
		Map<LocalDate, Integer> quizzesByDate = mapDailyCounts(quizResultRepository.countSubmittedGroupedByDate(range.startInstant(), range.endExclusive()));
		Map<LocalDate, Integer> questionsByDate = questionRepository.countCreatedByCategoryBetween(range.startInstant(), range.endExclusive())
				.stream()
				.collect(java.util.stream.Collectors.groupingBy(
						row -> row[0] instanceof java.sql.Date ? ((java.sql.Date) row[0]).toLocalDate() : LocalDate.now(),
						java.util.stream.Collectors.summingInt(row -> row[2] instanceof Number ? ((Number) row[2]).intValue() : 0)
				));
		
		for (LocalDate day = range.startDate(); !day.isAfter(range.endDate()); day = day.plusDays(1)) {
			csv.append(day.toString()).append(",");
			csv.append(usersByDate.getOrDefault(day, 0)).append(",");
			csv.append(quizzesByDate.getOrDefault(day, 0)).append(",");
			csv.append(questionsByDate.getOrDefault(day, 0)).append("\n");
		}
		
		return csv.toString();
	}

	private boolean inRange(Instant instant, Instant start, Instant end) {
		return instant != null && !instant.isBefore(start) && instant.isBefore(end);
	}

	private String safeMode(String mode) {
		return mode == null ? "adaptive" : mode.toLowerCase(Locale.ROOT);
	}

	private List<RecentActivity> buildRecentActivity(List<Student> students, List<QuizResult> results, List<Question> questions) {
		List<RecentActivity> activity = new ArrayList<>();

		students.stream()
				.limit(4)
				.forEach(student -> activity.add(new RecentActivity(
						student.getFullName(),
						"registered",
						student.getCreatedAt(),
						"/admin/users/" + student.getId()
				)));

		results.stream()
				.limit(4)
				.forEach(result -> activity.add(new RecentActivity(
						result.getStudent().getFullName(),
						"completed quiz",
						result.getSubmittedAt(),
						"/admin/reports"
				)));

		questions.stream()
				.limit(4)
				.forEach(question -> activity.add(new RecentActivity(
						"Question Bank",
						"added " + question.getCategory() + " question",
						question.getCreatedAt(),
						"/admin/questions/" + question.getId()
				)));

		return activity.stream()
				.sorted(Comparator.comparing(RecentActivity::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(8)
				.toList();
	}

	private List<CategorySummary> buildCategorySummaries() {
		return categoryRepository.findAll().stream()
				.map(category -> new CategorySummary(
						category.getId(),
						category.getName(),
						questionRepository.countByCategoryEntityId(category.getId()),
						category.getColor(),
						category.getCreatedAt(),
						category.getDescription()
				))
				.sorted(Comparator.comparing(CategorySummary::name))
				.toList();
	}

	private Category resolveCategory(String name) {
		String normalized = normalizeCategoryName(name);
		return categoryRepository.findByNameIgnoreCase(normalized).orElseGet(() -> {
			Category category = new Category();
			category.setName(normalized);
			category.setColor(resolveCategoryColor(normalized, null));
			return categoryRepository.save(category);
		});
	}

	private String normalizeCategoryName(String name) {
		if (name == null || name.isBlank()) {
			return "General";
		}
		String trimmed = name.trim();
		if ("General".equalsIgnoreCase(trimmed)) {
			return "General";
		}
		return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1).toLowerCase(Locale.ROOT);
	}

	private String resolveCategoryColor(String name, String existingColor) {
		if (existingColor != null && !existingColor.isBlank()) {
			return existingColor;
		}
		String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16"};
		int hash = name == null ? 0 : Math.abs(name.hashCode());
		return colors[hash % colors.length];
	}

	private AdminQuestionForm toForm(Question question) {
		AdminQuestionForm form = new AdminQuestionForm();
		form.setId(question.getId());
		form.setQuestionText(question.getQuestionText());
		form.setOptionA(question.getOptionA());
		form.setOptionB(question.getOptionB());
		form.setOptionC(question.getOptionC());
		form.setOptionD(question.getOptionD());
		form.setCorrectOption(question.getCorrectOption());
		form.setCategoryId(question.getCategoryEntity() != null ? question.getCategoryEntity().getId() : null);
		form.setDifficultyBand(question.getDifficultyBand());
		form.setStatus(question.getStatus());
		return form;
	}

	private void applyForm(Question question, AdminQuestionForm form) {
		question.setQuestionText(form.getQuestionText());
		question.setOptionA(form.getOptionA());
		question.setOptionB(form.getOptionB());
		question.setOptionC(form.getOptionC());
		question.setOptionD(form.getOptionD());
		question.setCorrectOption(form.getCorrectOption());
		question.setCategoryEntity(form.getCategoryId() != null ? categoryRepository.findById(form.getCategoryId()).orElse(null) : null);
		question.setDifficultyBand(form.getDifficultyBand());
		question.setStatus(form.getStatus());
	}

	public record ReportRange(
			LocalDate startDate,
			LocalDate endDate,
			Instant startInstant,
			Instant endExclusive,
			LocalDate previousStartDate,
			LocalDate previousEndDate,
			Instant previousStartInstant,
			Instant previousEndExclusive
	) {
	}

	public record ReportSummaryResponse(List<MetricCard> metrics, String startDate, String endDate) {
	}

	public record ReportChartsResponse(List<DailyMetricPoint> userGrowth, List<DailyMetricPoint> quizCompletions) {
	}

	public record MetricCard(String key, String icon, String label, int value, int trend, boolean positive, String meta) {
	}

	public record DailyMetricPoint(String date, int value) {
	}

	public record ReportCategoryBar(Long id, String name, long questionCount, int percent, String color) {
	}

	public record ReportActivityItem(String id, String type, String actor, String action, String href, Instant time, String relativeTime) {
	}

	public record CategorySummary(Long id, String name, long questionCount, String color, Instant createdAt, String description) {
	}

	public record AdminNotification(
			String id,
			String type,
			String title,
			String message,
			String href,
			String actionLabel,
			Instant createdAt,
			String relativeTime,
			boolean isNew
	) {
	}

	public record RecentActivity(String actor, String action, Instant timestamp, String href) {
	}

	public record CategoryQuestionsResponse(Long id, String name, long totalQuestions, List<CategoryQuestionSummary> questions) {
	}

	public record CategoryQuestionSummary(Long id, String questionText, String difficulty, String createdAt) {
	}
}
