<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<head>
<title>и-кратко</title>
<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.6/jquery.min.js"></script>
<script type="text/javascript" src="/scripts/jquery.highlight-3.js"></script>
<link rel="shortcut icon" href="/images/favicon.png" />
<style type="text/css">
.highlight {
    color:red;
    text-decoration:line-through;
}
</style>
<%@ include file="analytics.jsp" %>
</head>
<c:if test="${error == 'singleWord'}">
    Въведеният текст трябва да се състои от повече от една дума. Проверката на единични думи (извън контекст) може да се извърши и от spellchecker. За грешките, неоткриваеми от spellchecker, е необходим контекст.<br /><br />
</c:if>
<div id="text">
    ${input}
</div>

<hr />
Други грешки, които не са пряко свързани с и/й: ${otherMistakes}
<br />
Собствени имена (не се проверяват): ${properNames}
<hr />
<c:if test="${isCorrect}">
<br />Няма намерени грешки
</c:if>
<br />
<a href="/">Нова проверка</a>
<br /><br />
<c:if test="${resultId != null}">
    <a href="#" onclick="reportIncorrectResult('${resultId}')">Сигнализирай за грешен резултат</a>
</c:if>

<script type="text/javascript">
$(document).ready(function() {
    var text = $("#text");
    <c:forEach items="${mistakes}" var="mistake">
        text.highlight('${mistake.word}');
    </c:forEach>
});
function reportIncorrectResult(resultId) {
    $.post("/reportIncorrectResult", {resultId:resultId}, function() {
        alert("Сигналът е записан. Благодаря за съдействието.");
    });
}
</script>