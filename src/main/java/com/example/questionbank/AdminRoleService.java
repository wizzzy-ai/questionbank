package com.example.questionbank;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class AdminRoleService {
	private final StudentRepository studentRepository;

	public AdminRoleService(StudentRepository studentRepository) {
		this.studentRepository = studentRepository;
	}

	@Transactional
	public void promoteToAdmin(Student actor, Long targetId) {
		requireSuperAdmin(actor);
		Student target = studentRepository.findById(targetId).orElseThrow();
		if (target.isDeleted() || target.isBanned() || target.isSuperAdmin() || target.isBootstrapAdmin()) {
			return;
		}
		target.setRole(StudentRole.ADMIN);
		studentRepository.save(target);
	}

	@Transactional
	public void demoteToUser(Student actor, Long targetId) {
		requireSuperAdmin(actor);
		Student target = studentRepository.findById(targetId).orElseThrow();
		if (target.isSuperAdmin() || target.isBootstrapAdmin()) {
			return;
		}
		target.setRole(StudentRole.USER);
		studentRepository.save(target);
	}

	@Transactional
	public Student transferSuperAdmin(Student actor, Long targetId) {
		requireSuperAdmin(actor);
		Student target = studentRepository.findById(targetId).orElseThrow();
		if (target.isDeleted() || target.isBanned() || target.isBootstrapAdmin() || Objects.equals(actor.getId(), target.getId())) {
			return actor;
		}
		target.setRole(StudentRole.SUPER_ADMIN);
		target.setBootstrapAdmin(false);
		actor.setRole(StudentRole.USER);
		studentRepository.save(target);
		studentRepository.save(actor);
		return target;
	}

	@Transactional
	public void deleteBootstrapAdmin(Student actor, Long targetId) {
		requireSuperAdmin(actor);
		Student target = studentRepository.findById(targetId).orElseThrow();
		if (!target.isBootstrapAdmin() || target.isSuperAdmin() || Objects.equals(actor.getId(), target.getId())) {
			return;
		}
		target.setRole(StudentRole.USER);
		target.setBootstrapAdmin(false);
		target.setDeleted(true);
		target.setDeletedAt(Instant.now());
		target.setBanned(true);
		target.setLeaderboardVisible(false);
		target.setVerificationToken(null);
		target.setVerificationTokenExpiresAt(null);
		target.setPasswordResetToken(null);
		target.setPasswordResetTokenExpiresAt(null);
		studentRepository.save(target);
	}

	@Transactional
	public void reconcileRoles(String bootstrapEmail) {
		List<Student> students = studentRepository.findAll();
		students.forEach(student -> {
			student.setRole(student.getRole());
			if (bootstrapEmail != null && !bootstrapEmail.isBlank()
					&& student.getEmail() != null
					&& student.getEmail().equalsIgnoreCase(bootstrapEmail.trim())) {
				student.setBootstrapAdmin(true);
			}
		});

		List<Student> currentSuperAdmins = students.stream()
				.filter(Student::isSuperAdmin)
				.sorted(byOldestAccount())
				.toList();
		if (currentSuperAdmins.size() > 1) {
			Student keeper = chooseSuperAdminKeeper(currentSuperAdmins, bootstrapEmail);
			currentSuperAdmins.stream()
					.filter(student -> !Objects.equals(student.getId(), keeper.getId()))
					.forEach(student -> student.setRole(StudentRole.ADMIN));
		}

		boolean hasSuperAdmin = students.stream().anyMatch(Student::isSuperAdmin);
		if (!hasSuperAdmin) {
			Student keeper = chooseSuperAdminKeeper(students.stream()
					.filter(Student::isAdmin)
					.sorted(byOldestAccount())
					.toList(), bootstrapEmail);
			if (keeper != null) {
				keeper.setRole(StudentRole.SUPER_ADMIN);
			}
		}

		studentRepository.saveAll(students);
	}

	private Student chooseSuperAdminKeeper(List<Student> candidates, String bootstrapEmail) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		return candidates.stream()
				.filter(Student::isBootstrapAdmin)
				.findFirst()
				.or(() -> candidates.stream()
						.filter(student -> bootstrapEmail != null
								&& student.getEmail() != null
								&& student.getEmail().equalsIgnoreCase(bootstrapEmail.trim()))
						.findFirst())
				.orElse(candidates.get(0));
	}

	private Comparator<Student> byOldestAccount() {
		return Comparator.comparing(Student::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Student::getId, Comparator.nullsLast(Comparator.naturalOrder()));
	}

	private void requireSuperAdmin(Student actor) {
		if (actor == null || !actor.isSuperAdmin()) {
			throw new IllegalStateException("Super admin access required");
		}
	}
}
