# Android **_MVVM_** architecture

## **Contents**

### **1. MVVM architecture**

1. [MVC, MVP & MVVM](./mvvm/mvc_mvp_mvvm.md)
2. [UDF](./mvvm/udf.md) - Undirectional Data Flow

### **2. `ViewModel`**

1. [ViewModel](./mvvm/viewmodel.md) & Configuration changes
2. [ViewModel creation & delegation](./mvvm/viewmodel_creation_delegation.md) - create `ViewModel` class, delegate with `by viewModels()`/`by activityViewModels()`/`by navGraphViewModels()`
3. [ViewModelProvider.Factory](./mvvm/viewmodel_factory.md) - create `ViewModel` with **parameters**
4. [AndroidViewModel](./mvvm/androidviewmodel.md) - `ViewModel` with `Application` context

### 3. **`LiveData`**, **`StateFlow` & `SharedFlow`**

1. [LiveData](./mvvm/livedata.md)
2. [`Flow` replaces `LiveData`](./mvvm/flow_and_livedata.md)
3. [StateFlow](./mvvm/stateflow.md): `StateFlow`, convert LiveData (`asFlow()`), Flow ([`stateIn()`](./mvvm/stateflow.md#4-convert-flow-into-stateflow-statein)) to `StateFlow`, ...
4. [SharedFlow](./mvvm/sharedflow.md)
5. [`stateIn()`, `shareIn()`](./mvvm/convert_from_flow.md)
6. [`collectAsState`, `repeatOnLifecycle`, `flowWithLifecycle`](./mvvm/lifecycle_aware.md)

### 4. `viewModelScope`, `lifecycleScope` & **Dispatchers**: [Detail](./mvvm/scopes.md)

### 5. Others

1. [UI State Management](./mvvm/ui_states_management.md)
2. [`Repository` pattern](./mvvm/repository_pattern.md)<br/>
   Related topic:
   - [Offline-first architecture](https://developer.android.com/topic/architecture/data-layer/offline-first)
   - [Offline-first sync patterns](https://developersvoice.com/blog/mobile/offline-first-sync-patterns/)
   - [Offline-first in Flutter](https://docs.flutter.dev/app-architecture/design-patterns/offline-first)
3. [Data Binding](./mvvm/data_binding.md): DataBinding, custom attribute, Two-way binding với custom attributes...

---

_#vduczz_
