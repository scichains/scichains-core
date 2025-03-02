/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.executors.api.system;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.executors.api.settings.SettingsSpecification;
import net.algart.json.Jsons;

import java.util.*;

/**
 * Specification tree of executors, playing a role of settings:
 * real {@link SettingsSpecification settings}, mappings and so on.
 */
public final class SettingsTree {
    public final class Path {
        private final String[] names;

        private Path(String[] names) {
            this.names = Objects.requireNonNull(names, "Null names");
            checkNames();
        }

        private Path(Collection<String> parents, String controlName) {
            Objects.requireNonNull(parents, "Null parents");
            Objects.requireNonNull(controlName, "Null control name");
            this.names = parents.toArray(new String[Math.addExact(parents.size(), 1)]);
            this.names[this.names.length - 1] = controlName;
            checkNames();
        }

        private Path(Collection<String> names) {
            Objects.requireNonNull(names, "Null names");
            this.names = names.toArray(new String[0]);
            checkNames();
        }

        public List<String> names() {
            return List.of(names);
        }

        public Path parent() {
            if (names.length == 0) {
                return null;
            }
            return new Path(Arrays.copyOf(names, names.length - 1));
        }

        public Path child(String name) {
            Objects.requireNonNull(name, "Null name");
            String[] childNames = Arrays.copyOf(names, Math.addExact(names.length, 1));
            childNames[childNames.length - 1] = name;
            return new Path(childNames);
        }

        public ControlSpecification getControl() {
            if (names.length == 0) {
                throw new IllegalStateException("The root path \"" + this + "\" has no corresponding control");
            }
            final SettingsTree tree = getSubTree(names.length - 1);
            ControlSpecification control = tree.specification.getControl(names[names.length - 1]);
            if (control == null) {
                throw new IllegalStateException("Path \"" + this +
                        "\" does not exist: no control \"" + names[names.length - 1] + "\"");
            }
            return control;
        }

        public SettingsTree getSubTree() {
            return getSubTree(names.length);
        }

        private SettingsTree getSubTree(int n) {
            SettingsTree tree = SettingsTree.this;
            for (int i = 0; i < n; i++) {
                String name = names[i];
                tree = tree.subTrees.get(name);
                if (tree == null) {
                    throw new IllegalStateException("Path \"" + this +
                            "\" does not exist: no sub-tree \"" + name + "\"");
                }
            }
            return tree;
        }

        @Override
        public String toString() {
            return "/" + String.join("/", names);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Path path = (Path) o;
            return Objects.deepEquals(names, path.names);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(names);
        }

        private void checkNames() {
            for (String name : this.names) {
                Objects.requireNonNull(name, "Null name in the path");
            }
        }
    }

    private static final System.Logger LOG = System.getLogger(SettingsTree.class.getName());

    private final SmartSearchSettings smartSearch;
    private final ExecutorSpecificationFactory factory;
    private final ExecutorSpecification specification;
    private final Map<String, SettingsTree> subTrees = new LinkedHashMap<>();
    private final SettingsTree parent;
    private final Path path;
    private final List<Path> treePaths = new ArrayList<>();
    private final List<Path> controlPaths = new ArrayList<>();
    private final boolean complete;

    private SettingsTree(
            SmartSearchSettings smartSearch,
            ExecutorSpecificationFactory factory,
            ExecutorSpecification specification,
            SettingsTree parent,
            List<String> currentPath,
            Set<String> stackForDetectingRecursion) {
        this.smartSearch = smartSearch;
        this.factory = Objects.requireNonNull(factory, "Null specification factory");
        assert smartSearch == null || smartSearch.factory() == factory;
        this.specification = Objects.requireNonNull(specification, "Null settings specification");
        this.parent = parent;
        this.path = new Path(currentPath);
        this.complete = buildTree(currentPath, stackForDetectingRecursion);
    }

    private SettingsTree(
            SmartSearchSettings smartSearch,
            ExecutorSpecificationFactory factory,
            ExecutorSpecification specification,
            SettingsTree parent) {
        this(smartSearch, factory, specification, parent, new ArrayList<>(), new HashSet<>());
    }

    public static SettingsTree of(SmartSearchSettings smartSearch, ExecutorSpecification specification) {
        Objects.requireNonNull(smartSearch, "Null smart search");
        return new SettingsTree(smartSearch, smartSearch.factory(), specification, null);
    }

    public static SettingsTree of(ExecutorSpecificationFactory factory, ExecutorSpecification specification) {
        return new SettingsTree(null, factory, specification, null);
    }

    public Path newPath(Collection<String> names) {
        return new Path(names);
    }

    public SettingsTree parent() {
        return parent;
    }

    /**
     * Returns the path to this tree. Will be empty in the root tree and non-empty for its subtrees.
     *
     * @return the path to this tree.
     */
    public Path path() {
        return path;
    }

    public ExecutorSpecificationFactory factory() {
        return factory;
    }

    public ExecutorSpecification specification() {
        return specification;
    }

    public Map<String, SettingsTree> subTrees() {
        return Collections.unmodifiableMap(subTrees);
    }

    public List<Path> treePaths() {
        return Collections.unmodifiableList(treePaths);
    }

    public List<Path> controlPaths() {
        return Collections.unmodifiableList(controlPaths);
    }

    public int numberOfTrees() {
        return treePaths.size();
    }

    public String id() {
        return specification.getId();
    }

    /**
     * Returns <code>true</code> if all children are successfully found by
     * {@link ControlSpecification#getSettingsId() settings ID} and
     * all subtrees are also complete.
     *
     * @return whether this tree is complete.
     */
    public boolean isComplete() {
        return complete;
    }

    public JsonObject specificationJson() {
        return specificationJson(ExecutorSpecification.JsonMode.MEDIUM);
        // - by default, there are no reasons to duplicate settings information in the tree
    }

    public JsonObject specificationJson(ExecutorSpecification.JsonMode mode) {
        specification.checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        specification.buildJson(builder, mode, name -> childJsonTree(name, mode));
        return builder.build();
    }

    public JsonObject defaultSettingsJson() {
        specification.checkCompleteness();
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        specification.buildDefaultSettingsJson(builder, this::childDefaultSettingsJsonTree);
        return builder.build();
    }

    public String jsonString() {
        return Jsons.toPrettyString(specificationJson());
    }

    public String jsonString(ExecutorSpecification.JsonMode mode) {
        return Jsons.toPrettyString(specificationJson(mode));
    }

    public String defaultSettingsJsonString() {
        return Jsons.toPrettyString(defaultSettingsJson());
    }

    @Override
    public String toString() {
        return "settings tree" + (smartSearch != null ? " (smart)" : "") + " for executor " +
                "\"" + specification.canonicalName() + "\" (" + specification.getId() + ")" +
                ": " + subTrees.size() + " children";
    }

    private boolean buildTree(List<String> parentPath, Set<String> stackForDetectingRecursion) {
        Map<String, SettingsTree> children = new LinkedHashMap<>();
        boolean complete = true;
        stackForDetectingRecursion.add(specification.getId());
        treePaths.add(new Path(parentPath));
        try {
            for (var entry : specification.getControls().entrySet()) {
                final String name = entry.getKey();
                final ControlSpecification control = entry.getValue();
                controlPaths.add(new Path(parentPath, control.getName()));
                if (control.isSubSettings()) {
                    String settingsId = control.getSettingsId();
                    if (settingsId == null && smartSearch != null) {
                        smartSearch.search();
                        // ...and try again!
                        settingsId = control.getSettingsId();
                    }
                    final boolean found = settingsId != null;
                    complete &= found;
                    if (found) {
                        final ExecutorSpecification childSettings = factory.getSpecification(settingsId);
                        if (childSettings == null) {
                            throw new IllegalStateException("Child settings with ID \"" + settingsId +
                                    "\" (child control \"" + name + "\") is not found");
                        }
                        if (stackForDetectingRecursion.contains(settingsId)) {
                            throw new IllegalStateException("Cannot build tree due to recursive link to " +
                                    "settings ID \"" + settingsId +
                                    "\" (child control \"" + name +
                                    "\") detected in the settings with ID \"" + specification.getId() +
                                    "\" (\"" + specification.getName() +
                                    "\" in category \"" + specification.getCategory() + "\")");
                        }
                        final List<String> childPath = new ArrayList<>(parentPath);
                        childPath.add(control.getName());
                        final SettingsTree child = new SettingsTree(
                                smartSearch,
                                factory,
                                childSettings,
                                this,
                                childPath,
                                stackForDetectingRecursion);
                        treePaths.addAll(child.treePaths);
                        controlPaths.addAll(child.controlPaths);
                        complete &= child.complete;
                        children.put(name, child);
                    } else {
                        LOG.log(System.Logger.Level.DEBUG, () -> "Sub-settings " + name + " not found");
                    }
                }
            }
        } finally {
            stackForDetectingRecursion.remove(specification.getId());
        }
        // Note: we did not modify this object before this moment to make this method atomic regarding a failure
        this.subTrees.clear();
        this.subTrees.putAll(children);
        return complete;
    }

    /*
    void buildSettingsJson(
            JsonObjectBuilder builder,
            Function<Path, JsonObject> controlJsonBuilder) {
        Objects.requireNonNull(builder, "Null builder");
        for (Map.Entry<String, ControlSpecification> entry : specification.controls.entrySet()) {
            final String name = entry.getKey();
            final SettingsTree subTree = subTrees.get(name);
            if (subTree != null) {
                JsonObjectBuilder subTreeBuilder = Json.createObjectBuilder();
                buildSettingsJson(subTreeBuilder, controlJsonBuilder);
                builder.add(name, subTreeBuilder.build());
            } else {

            }
            final ControlSpecification control = entry.getValue();
            control.checkCompleteness();

            if (controlJsonBuilder != null) {
                if (ExecutorSpecification.SETTINGS.equals(name)) {
                    // - no sense to include this parameter while building tree:
                    // this should be filled by resulting JSON tree
                    continue;
                }

                if (control.isSubSettings()) {
                    final JsonObject subSettingsJson = controlJsonBuilder.apply(name);
                    if (subSettingsJson != null) {
                        builder.add(SettingsSpecification.settingsKey(name), subSettingsJson);
                        continue;
                    }
                }
            }
            if (control.hasDefaultJsonValue()) {
                builder.add(name, control.getDefaultJsonValue());
            }
        }
    }
     */

    private JsonObject childJsonTree(String name, ExecutorSpecification.JsonMode mode) {
        return subTrees.containsKey(name) ? subTrees.get(name).specificationJson(mode) : null;
    }

    private JsonObject childDefaultSettingsJsonTree(String name) {
        return subTrees.containsKey(name) ? subTrees.get(name).defaultSettingsJson() : null;
    }
}
