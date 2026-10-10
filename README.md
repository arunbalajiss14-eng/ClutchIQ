<<<<<<< HEAD
# ClutchIQ — AI Performance Coach

Sports performance and team management platform (Spring Boot 3.5, Java 17+ or 21, MySQL, Thymeleaf).

## Run it (VS Code)
1. Start MySQL (MySQL Workbench should show your server running).
2. Open `src/main/resources/application.properties` and set `spring.datasource.username` and
   `spring.datasource.password` to your MySQL login. The `clutchiq`/`mevsme` database and all tables are created automatically.
3. Open the project folder in VS Code. Install "Extension Pack for Java", "Spring Boot Extension Pack"
   and "Lombok Annotations Support for VS Code" when VS Code suggests them.
4. If you're on Java 17, change `<java.version>21</java.version>` to `17` in `pom.xml` first.
5. In a terminal inside the project folder run:

       mvn clean spring-boot:run

6. Open http://localhost:8080

## Demo logins (password: demo123)
- coach@demo.com    Coach (Chennai Strikers, cricket)
- rahul@demo.com    Player with real misses, so the AI Performance Coach has adjustments to make
- arjun@demo.com    Player who is consistent

Set `app.seed-demo=false` in application.properties for an empty database.

## How the parts map to the spec
- One login, two contexts: Coach <-> Team, Player <-> Team (switcher at top left).
- Coach-only data: `/coach/**` is locked to the COACH role; players have no endpoint that writes assessments.
- **AI Performance Coach** (`AiCoachService`): turns the coach's weakness note into today's priorities,
  scores match readiness from training/recovery/sleep/energy/recent form, reviews yesterday's duty
  completion, and runs a ClutchAI-driven pressure drill. Player page: `/player/coach`. Coach sees the
  same recommendation on a player's detail page.
- ClutchAI (`ClutchService`): finds which match situations a player over- or under-performs in; feeds
  both its own insights panel and the AI Performance Coach's pressure drill.
- Upcoming Match (`CoachMatchController`, `PlayerController`): coach creates/edits matches (opponent, date, time, place, minimum age, fee), picks the squad ranked by overall %, and sees who has confirmed. Players see match details and confirm themselves. The public ranking link lives here too.
- Move a player between teams: on a player's detail page, a coach with more than one team can move them (every team shares the coach's one sport).
- Routine history (`HistoryService`): 4-week view of daily routine, sleep and energy with charts.
- Overall % (`ScoreService`): coach rating 40% + daily routine 30% + last match rating 30%. Used to pick match squads (Team > Schedule) and for the public ranking link (Players page).
- Daily duties (`DailyLog`/`/player/routine`): the check-ins that both the AI Coach and match readiness
  are computed from.

## Moderation and admin
- Remove a player: Players page > Remove. They can no longer sign in; their routine, performance and
  match history stay intact. Restore them from Players > Deleted players.
- Team: rename or delete from the Teams page. Delete is blocked while the team still has active players.
- Upcoming Match: delete a match from either Upcoming Match or Results.
- Training and tournaments: delete from the Schedule page.
- Discussion: a coach can delete any message (Discussion page). The row is kept but the text is hidden,
  so the thread still reads naturally.
=======
# ClutchIQ
>>>>>>> origin/main
