# KMP Study Book Index

This folder is organized for focused study. You can read the full book in [`KMP_STUDY_BOOK.md`](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/KMP_STUDY_BOOK.md), but the best learning path is to study one chapter at a time.

## How To Use This Book

Read in this flow:

1. Start with the preface.
2. Read one chapter at a time.
3. After each chapter, open the actual files from the repository and map the chapter to real code.
4. Keep notes for your own history app after Chapters 6, 7, 12, 18, and 24.

Good milestone checkpoints:

- After Chapter 3, you should understand how the project is wired.
- After Chapter 6, you should understand shared state management.
- After Chapter 12, you should understand Android startup and lifecycle.
- After Chapter 18, you should understand how iOS consumes shared Kotlin code.
- After Chapter 24, you should be ready to design your own app structure.

## Start Here

- [Preface](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/00-Preface.md)

## Chapters

1. [What This Repository Really Is](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/01-what-this-repository-really-is.md)
Focus: understand what KMP is, what this sample is teaching, and the main architecture mindset.

2. [The Top-Level Folder Structure](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/02-the-top-level-folder-structure.md)
Focus: learn what each root folder does and which ones matter most.

3. [The Build System and Project Wiring](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/03-the-build-system-and-project-wiring.md)
Focus: understand Gradle modules, source sets, version catalogs, and why iOS is handled differently.

4. [The Module Architecture](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/04-the-module-architecture.md)
Focus: understand how `shared`, `composeApp`, and `iosApp` relate to each other.

5. [The Shared Module in Detail](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/05-the-shared-module-in-detail.md)
Focus: learn why the shared module is the heart of the project and how its packages are organized.

6. [`FeedStore` and Shared State Management](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/06-feedstore-and-shared-state-management.md)
Focus: master state, actions, side effects, and the app’s central runtime flow.

7. [`RssReader` as the Shared Business Layer](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/07-rssreader-as-the-shared-business-layer.md)
Focus: understand cache-first logic, orchestration, and how shared business code is designed.

8. [Networking with `FeedLoader` and `HttpClient`](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/08-networking-with-feedloader-and-httpclient.md)
Focus: understand Ktor, XML handling, and multiplatform networking setup.

9. [Local Storage with `FeedStorage`](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/09-local-storage-with-feedstorage.md)
Focus: learn how persistence works and how the same storage abstraction uses different platform backends.

10. [The Domain Model and RSS XML Mapping](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/10-the-domain-model-and-rss-xml-mapping.md)
Focus: understand how RSS XML becomes Kotlin data classes and why the annotations matter.

11. [Dependency Injection in This Project](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/11-dependency-injection-in-this-project.md)
Focus: understand how Koin builds the dependency graph differently on Android, iOS, and desktop.

12. [Android App Lifecycle in This Repository](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/12-android-app-lifecycle-in-this-repository.md)
Focus: follow Android from manifest to `Application` to `Activity` to Compose to shared state.

13. [Compose UI Architecture in `composeApp`](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/13-compose-ui-architecture-in-composeapp.md)
Focus: understand the root Compose shell, navigation, screen organization, and snackbar handling.

14. [The Main Feed Screen](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/14-the-main-feed-screen.md)
Focus: understand the main user-facing screen, filtering, and post-opening behavior.

15. [Feed Management Screen](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/15-feed-management-screen.md)
Focus: understand adding and deleting feeds and how that connects back into shared state.

16. [Compose UI Components and Utility Files](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/16-compose-ui-components-and-utility-files.md)
Focus: understand dialogs, cards, icons, previews, and theme support files.

17. [Desktop Support](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/17-desktop-support.md)
Focus: understand how the same shared logic and Compose UI can also run on desktop.

18. [iOS App Architecture](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/18-ios-app-architecture.md)
Focus: understand how SwiftUI consumes the Kotlin framework and how the store is bridged into iOS.

19. [The iOS Bridge Files in `shared/src/iosMain`](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/19-the-ios-bridge-files-in-shared-src-iosmain.md)
Focus: understand `CFlow`, watch helpers, and interop adaptation.

20. [Resources, Assets, and UI Metadata](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/20-resources-assets-and-ui-metadata.md)
Focus: understand what is shared resource data and what remains native per platform.

21. [Continuous Integration and Testing Reality](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/21-continuous-integration-and-testing-reality.md)
Focus: understand what the repo verifies automatically and where its testing is still minimal.

22. [End-to-End Runtime Data Flow](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/22-end-to-end-runtime-data-flow.md)
Focus: trace first launch, refresh, add, delete, and error paths from start to finish.

23. [Why This Repository Is Good and Where It Is Simple](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/23-why-this-repository-is-good-and-where-it-is-simple.md)
Focus: separate strong architecture ideas from sample-project shortcuts.

24. [How to Adapt This Repo for Your History App](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/24-how-to-adapt-this-repo-for-your-history-app.md)
Focus: translate the architecture into your own domain and feature planning.

25. [A Practical Reading Order for Learning](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/25-a-practical-reading-order-for-learning.md)
Focus: follow the best code-reading order once you start learning directly from source files.

26. [Frequently Confusing Points for Beginners](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/26-frequently-confusing-points-for-beginners.md)
Focus: clear up the common beginner confusions before they slow you down.

27. [Important Design Lessons You Should Carry Forward](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/27-important-design-lessons-you-should-carry-forward.md)
Focus: extract reusable design principles from the sample.

28. [Final Summary](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/chapters/28-final-summary.md)
Focus: reinforce the architecture in one short mental model.

## Appendices

- [Appendix A: Fast File Map](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/appendices/A-fast-file-map.md)
- [Appendix B: Suggested Next Learning Exercise](/Users/shreyasdamase/Vanguard/kmp-production-sample/study-book/appendices/B-suggested-next-learning-exercise.md)

## Suggested Weekly Study Plan

If you want a calm learning rhythm:

- Day 1: Chapters 1-3
- Day 2: Chapters 4-7
- Day 3: Chapters 8-12
- Day 4: Chapters 13-18
- Day 5: Chapters 19-24
- Day 6: Chapters 25-28 and appendices
- Day 7: revisit the repository and trace the real code paths yourself
