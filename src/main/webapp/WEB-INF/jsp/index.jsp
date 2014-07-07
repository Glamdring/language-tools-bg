<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <link rel="shortcut icon" href="/images/favicon.png" />
    <title>и-кратко</title>
    <%@ include file="analytics.jsp" %>
</head>
<body>
<div align="center">
    <img src="/images/logo.png" alt="Й" title="Й" />
    <form action="/submit"" method="POST">
    Текст за проверка<br />
    <textarea name="text" rows="15" cols="45"></textarea>
    <br /><input type="submit" value="Провери" />
    </form>

    <div style="font-size: 0.8em; margin-top: 50px;">Проектът е в експериментален стадий. Целта му е да предостави услуга за проверка на използването на "и" и "й" на места, където те често се бъркат, тъй като обикновен spellcheck не винаги може да открие такива грешки.</div>
    <div style="font-size: 0.8em;">автор: <a href="http://bozho.net">Божидар Божанов</a></div>
</div>
</body>
</html>