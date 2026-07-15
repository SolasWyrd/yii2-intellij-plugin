# Yii2 Model Magic

Плагин для PhpStorm, который добавляет поддержку magic properties моделей Yii2 при обращении через `$model->property`.

Проект находится в активной разработке. Текущий scope ограничен model magic; это не полный набор инструментов для views, routes, translations, aliases или application components.

## Реализовано

- автодополнение свойств из public fields, getters, setters, `hasOne()` / `hasMany()`, `attributes()` и PHPDoc;
- переход к getter/setter/relation через Go to Declaration;
- вывод типа из PHP type declarations и PHPDoc;
- inspection неизвестных свойств модели;
- project settings для включения источников и настройки приоритетов resolution/navigation;
- нормализация snake_case и camelCase при поиске getter/setter.

## Пока не реализовано

- свойства, добавляемые Yii behaviors;
- `$model['property']` через `ArrayAccess`;
- Find Usages и rename refactoring для magic properties;
- генерация PHPDoc/getter/setter через quick fixes;
- поддержка views, routes, translations, aliases и `Yii::$app` components.

## Архитектура

```text
com.yii2storm.modelmagic/
├── resolver/      # Сбор и приоритизация magic properties
├── util/          # Определение model classes по PSI type
├── completion/    # Completion contributor
├── navigation/    # Go to Declaration
├── type/          # PhpTypeProvider4
├── inspection/    # Inspection неизвестных свойств
├── annotator/     # Информационная аннотация resolved properties
└── settings/      # Project-level sources и priorities
```

## Требования

- PhpStorm 2025.1 или новее;
- JDK 21 для локальной сборки;
- Gradle Wrapper из репозитория.

## Проверки

```bash
./gradlew test
./gradlew check
./gradlew buildPlugin
./gradlew verifyPlugin
```

`test` включает обычные unit tests и headless IntelliJ Platform tests на реальном PSI. `verifyPlugin` проверяет binary compatibility выбранных версий PhpStorm.

## Запуск в sandbox IDE

```bash
./gradlew runIde
```

## Сборка

```bash
./gradlew buildPlugin
```

Архив создаётся в `build/distributions/yii2-model-magic-1.0.0.zip`.

Установка: **Settings → Plugins → ⚙ → Install Plugin from Disk**.

## Ближайшее развитие

1. Расширить PSI fixtures для relations, PHPDoc variants и inherited models.
2. Корректно разбирать relation target и cardinality `hasOne()` / `hasMany()`.
3. Реализовать Find Usages/rename через совместимый с PHP PSI контракт.
4. Добавить behavior properties и отдельную поддержку ArrayAccess.
5. Реализовать безопасные quick fixes с проверяемыми PSI transformations.
