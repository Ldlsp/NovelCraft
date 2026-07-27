package com.mozhou.novelcraft

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ProjectBackupSnapshot(
    val project: NovelProject,
    val chapters: List<Chapter>,
    val revisions: List<ChapterRevision>,
    val autoWriteRuns: List<AutoWriteRun>,
    val items: List<StoryItem>,
    val mentions: List<ChapterStoryMention>,
    val notes: List<ResearchNote>,
    val editorialReviews: List<EditorialReview>,
    val anchors: List<StoryAnchor>,
    val edges: List<StoryEdge>,
    val pacingEvents: List<ChapterPacingEvent>,
    val batchReviews: List<BatchReviewRun>,
    val reviewIssues: List<ReviewIssue>,
    val styleProfiles: List<StyleProfile>,
    val eventMatrixRules: List<EventMatrixRule> = emptyList(),
    val gateReports: List<ChapterGateReport> = emptyList(),
)

/** Versioned, portable backup. IDs are deliberately restored as new IDs by the repository. */
object ProjectBackupCodec {
    private const val FORMAT = "novelcraft-project-backup"
    private const val VERSION = 1

    fun encode(snapshot: ProjectBackupSnapshot): String = JSONObject().apply {
        put("format", FORMAT); put("version", VERSION)
        put("project", project(snapshot.project))
        put("coverBase64", snapshot.project.coverPath.takeIf { File(it).isFile }?.let { Base64.encodeToString(File(it).readBytes(), Base64.NO_WRAP) } ?: "")
        put("chapters", array(snapshot.chapters) { chapter(it) })
        put("revisions", array(snapshot.revisions) { revision(it) })
        put("autoWriteRuns", array(snapshot.autoWriteRuns) { autoRun(it) })
        put("items", array(snapshot.items) { item(it) })
        put("mentions", array(snapshot.mentions) { mention(it) })
        put("notes", array(snapshot.notes) { note(it) })
        put("editorialReviews", array(snapshot.editorialReviews) { editorial(it) })
        put("anchors", array(snapshot.anchors) { anchor(it) })
        put("edges", array(snapshot.edges) { edge(it) })
        put("pacingEvents", array(snapshot.pacingEvents) { pacing(it) })
        put("batchReviews", array(snapshot.batchReviews) { batch(it) })
        put("reviewIssues", array(snapshot.reviewIssues) { issue(it) })
        put("styleProfiles", array(snapshot.styleProfiles) { style(it) })
        put("eventMatrixRules", array(snapshot.eventMatrixRules) { eventRule(it) })
        put("gateReports", array(snapshot.gateReports) { gateReport(it) })
    }.toString(2)

    fun decode(raw: String): ProjectBackupSnapshot {
        val root = JSONObject(raw)
        require(root.optString("format") == FORMAT) { "不是墨舟项目备份包" }
        require(root.optInt("version") == VERSION) { "不支持的备份版本" }
        return ProjectBackupSnapshot(
            project = project(root.getJSONObject("project")), chapters = list(root.optJSONArray("chapters"), ::chapter),
            revisions = list(root.optJSONArray("revisions"), ::revision), autoWriteRuns = list(root.optJSONArray("autoWriteRuns"), ::autoRun),
            items = list(root.optJSONArray("items"), ::item), mentions = list(root.optJSONArray("mentions"), ::mention),
            notes = list(root.optJSONArray("notes"), ::note), editorialReviews = list(root.optJSONArray("editorialReviews"), ::editorial),
            anchors = list(root.optJSONArray("anchors"), ::anchor), edges = list(root.optJSONArray("edges"), ::edge),
            pacingEvents = list(root.optJSONArray("pacingEvents"), ::pacing), batchReviews = list(root.optJSONArray("batchReviews"), ::batch),
            reviewIssues = list(root.optJSONArray("reviewIssues"), ::issue),
            styleProfiles = list(root.optJSONArray("styleProfiles"), ::style),
            eventMatrixRules = list(root.optJSONArray("eventMatrixRules"), ::eventRule),
            gateReports = list(root.optJSONArray("gateReports"), ::gateReport),
        )
    }

    fun coverData(raw: String): ByteArray? = JSONObject(raw).optString("coverBase64").takeIf { it.isNotBlank() }?.let { Base64.decode(it, Base64.DEFAULT) }
    private fun <T> array(values: List<T>, convert: (T) -> JSONObject) = JSONArray().also { out -> values.forEach { out.put(convert(it)) } }
    private fun <T> list(values: JSONArray?, convert: (JSONObject) -> T): List<T> = buildList { for (i in 0 until (values?.length() ?: 0)) add(convert(values!!.getJSONObject(i))) }
    private fun JSONObject.s(key: String) = optString(key, "")
    private fun JSONObject.l(key: String) = optLong(key, 0)

    private fun project(v: NovelProject) = JSONObject().apply { put("id",v.id);put("title",v.title);put("genre",v.genre);put("premise",v.premise);put("styleGuide",v.styleGuide);put("outlineRevisionReport",v.outlineRevisionReport);put("summary",v.summary);put("tags",v.tags);put("targetAudience",v.targetAudience);put("protagonistName",v.protagonistName);put("longFormBlueprint",v.longFormBlueprint);put("targetChapterCount",v.targetChapterCount);put("targetWordCount",v.targetWordCount);put("pacingProfile",v.pacingProfile);put("forbiddenContent",v.forbiddenContent);put("automationLevel",v.automationLevel);put("targetChapterWordCount",v.targetChapterWordCount);put("targetChapterWordCountMax",v.targetChapterWordCountMax);put("createdAt",v.createdAt);put("updatedAt",v.updatedAt) }
    private fun project(o: JSONObject) = NovelProject(
        id = o.l("id"), title = o.s("title"), genre = o.s("genre"), premise = o.s("premise"),
        styleGuide = o.s("styleGuide"), outlineRevisionReport = o.s("outlineRevisionReport"), summary = o.s("summary"), tags = o.s("tags"),
        targetAudience = o.s("targetAudience"), protagonistName = o.s("protagonistName"), longFormBlueprint = o.s("longFormBlueprint"),
        targetChapterCount = o.optInt("targetChapterCount"), targetWordCount = o.optInt("targetWordCount"), pacingProfile = o.s("pacingProfile").ifBlank { "均衡" },
        forbiddenContent = o.s("forbiddenContent"), automationLevel = o.s("automationLevel").ifBlank { "半自动" }, targetChapterWordCount = o.optInt("targetChapterWordCount", 3000), targetChapterWordCountMax = o.optInt("targetChapterWordCountMax", 5000),
        createdAt = o.l("createdAt"), updatedAt = o.l("updatedAt"),
    )
    private fun chapter(v: Chapter) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("number",v.number);put("title",v.title);put("content",v.content);put("outline",v.outline);put("beatSheet",v.beatSheet);put("targetWordCount",v.targetWordCount);put("qualityStatus",v.qualityStatus);put("qualityIssueSummary",v.qualityIssueSummary);put("lifecycleStatus",v.lifecycleStatus);put("lifecycleDetail",v.lifecycleDetail);put("memoryUpdatedAt",v.memoryUpdatedAt);put("autoWriteRunId",v.autoWriteRunId);put("gateFailureCount",v.gateFailureCount);put("requiresHumanReview",v.requiresHumanReview);put("updatedAt",v.updatedAt) }
    private fun chapter(o: JSONObject) = Chapter(
        id = o.l("id"), projectId = o.l("projectId"), number = o.optInt("number"), title = o.s("title"), content = o.s("content"),
        outline = o.s("outline"), beatSheet = o.s("beatSheet"), targetWordCount = o.optInt("targetWordCount"), qualityStatus = o.s("qualityStatus"),
        qualityIssueSummary = o.s("qualityIssueSummary"), lifecycleStatus = o.s("lifecycleStatus"), lifecycleDetail = o.s("lifecycleDetail"),
        memoryUpdatedAt = o.l("memoryUpdatedAt"), autoWriteRunId = o.l("autoWriteRunId"), gateFailureCount = o.optInt("gateFailureCount"),
        requiresHumanReview = o.optBoolean("requiresHumanReview"), updatedAt = o.l("updatedAt"),
    )
    private fun revision(v: ChapterRevision) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("chapterId",v.chapterId);put("previousContent",v.previousContent);put("reason",v.reason);put("createdAt",v.createdAt) }
    private fun revision(o: JSONObject) = ChapterRevision(o.l("id"),o.l("projectId"),o.l("chapterId"),o.s("previousContent"),o.s("reason"),o.l("createdAt"))
    private fun autoRun(v: AutoWriteRun) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("requestedCount",v.requestedCount);put("completedCount",v.completedCount);put("status",v.status);put("detail",v.detail);put("createdAt",v.createdAt);put("updatedAt",v.updatedAt) }
    private fun autoRun(o: JSONObject) = AutoWriteRun(o.l("id"),o.l("projectId"),o.optInt("requestedCount"),o.optInt("completedCount"),o.s("status"),o.s("detail"),o.l("createdAt"),o.l("updatedAt"))
    private fun item(v: StoryItem) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("kind",v.kind);put("name",v.name);put("detail",v.detail);put("status",v.status);put("updatedAt",v.updatedAt);put("cascadePending",v.cascadePending) }
    private fun item(o: JSONObject) = StoryItem(o.l("id"),o.l("projectId"),o.s("kind"),o.s("name"),o.s("detail"),o.s("status"),o.l("updatedAt"),o.optBoolean("cascadePending"))
    private fun mention(v: ChapterStoryMention) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("chapterId",v.chapterId);put("storyItemId",v.storyItemId);put("createdAt",v.createdAt) }
    private fun mention(o: JSONObject) = ChapterStoryMention(o.l("id"),o.l("projectId"),o.l("chapterId"),o.l("storyItemId"),o.l("createdAt"))
    private fun note(v: ResearchNote) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("title",v.title);put("sourceUrl",v.sourceUrl);put("tags",v.tags);put("content",v.content);put("rightsConfirmed",v.rightsConfirmed);put("createdAt",v.createdAt);put("updatedAt",v.updatedAt) }
    private fun note(o: JSONObject) = ResearchNote(o.l("id"),o.l("projectId"),o.s("title"),o.s("sourceUrl"),o.s("tags"),o.s("content"),o.optBoolean("rightsConfirmed"),o.l("createdAt"),o.l("updatedAt"))
    private fun editorial(v: EditorialReview) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("chapterId",v.chapterId);put("content",v.content);put("createdAt",v.createdAt) }
    private fun editorial(o: JSONObject) = EditorialReview(o.l("id"),o.l("projectId"),o.l("chapterId"),o.s("content"),o.l("createdAt"))
    private fun anchor(v: StoryAnchor) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("startChapter",v.startChapter);put("endChapter",v.endChapter);put("title",v.title);put("coreConflict",v.coreConflict);put("allowedPlot",v.allowedPlot);put("forbiddenReveals",v.forbiddenReveals);put("mandatoryTension",v.mandatoryTension);put("cascadePending",v.cascadePending) }
    private fun anchor(o: JSONObject) = StoryAnchor(o.l("id"),o.l("projectId"),o.optInt("startChapter"),o.optInt("endChapter"),o.s("title"),o.s("coreConflict"),o.s("allowedPlot"),o.s("forbiddenReveals"),o.s("mandatoryTension"),o.optBoolean("cascadePending"))
    private fun edge(v: StoryEdge) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("sourceItemId",v.sourceItemId);put("targetItemId",v.targetItemId);put("relation",v.relation);put("strength",v.strength);put("description",v.description);put("sinceChapter",v.sinceChapter);put("cascadePending",v.cascadePending) }
    private fun edge(o: JSONObject) = StoryEdge(o.l("id"),o.l("projectId"),o.l("sourceItemId"),o.l("targetItemId"),o.s("relation"),o.optDouble("strength",0.5).toFloat(),o.s("description"),o.optInt("sinceChapter",1),o.optBoolean("cascadePending"))
    private fun pacing(v: ChapterPacingEvent) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("chapterId",v.chapterId);put("chapterNumber",v.chapterNumber);put("eventType",v.eventType);put("pace",v.pace);put("note",v.note);put("createdAt",v.createdAt) }
    private fun pacing(o: JSONObject) = ChapterPacingEvent(o.l("id"),o.l("projectId"),o.l("chapterId"),o.optInt("chapterNumber"),o.s("eventType"),o.s("pace"),o.s("note"),o.l("createdAt"))
    private fun batch(v: BatchReviewRun) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("startChapter",v.startChapter);put("endChapter",v.endChapter);put("round",v.round);put("report",v.report);put("createdAt",v.createdAt) }
    private fun batch(o: JSONObject) = BatchReviewRun(o.l("id"),o.l("projectId"),o.optInt("startChapter"),o.optInt("endChapter"),o.optInt("round",1),o.s("report"),o.l("createdAt"))
    private fun issue(v: ReviewIssue) = JSONObject().apply { put("id",v.id);put("projectId",v.projectId);put("reviewRunId",v.reviewRunId);put("chapterNumber",v.chapterNumber);put("severity",v.severity);put("summary",v.summary);put("status",v.status);put("createdAt",v.createdAt) }
    private fun issue(o: JSONObject) = ReviewIssue(o.l("id"),o.l("projectId"),o.l("reviewRunId"),o.optInt("chapterNumber"),o.s("severity"),o.s("summary"),o.s("status"),o.l("createdAt"))
    private fun style(v: StyleProfile) = JSONObject().apply { put("id",v.id);put("name",v.name);put("genre",v.genre);put("guide",v.guide);put("sourceProjectId",v.sourceProjectId);put("metrics",v.metrics);put("keywords",v.keywords);put("createdAt",v.createdAt) }
    private fun style(o: JSONObject) = StyleProfile(o.l("id"),o.s("name"),o.s("genre"),o.s("guide"),o.l("sourceProjectId"),o.s("metrics"),o.s("keywords"),o.l("createdAt"))
    private fun eventRule(v: EventMatrixRule) = JSONObject().apply { put("id", v.id); put("projectId", v.projectId); put("ruleKey", v.ruleKey); put("label", v.label); put("cooldown", v.cooldown); put("category", v.category); put("enabled", v.enabled); put("createdAt", v.createdAt) }
    private fun eventRule(o: JSONObject) = EventMatrixRule(id = o.l("id"), projectId = o.l("projectId"), ruleKey = o.s("ruleKey"), label = o.s("label"), cooldown = o.optInt("cooldown", 2), category = o.s("category"), enabled = o.optBoolean("enabled", true), createdAt = o.l("createdAt"))
    private fun gateReport(v: ChapterGateReport) = JSONObject().apply { put("id", v.id); put("projectId", v.projectId); put("chapterId", v.chapterId); put("stage", v.stage); put("passed", v.passed); put("content", v.content); put("contextSnapshot", v.contextSnapshot); put("createdAt", v.createdAt) }
    private fun gateReport(o: JSONObject) = ChapterGateReport(id = o.l("id"), projectId = o.l("projectId"), chapterId = o.l("chapterId"), stage = o.s("stage"), passed = o.optBoolean("passed"), content = o.s("content"), contextSnapshot = o.s("contextSnapshot"), createdAt = o.l("createdAt"))
}
