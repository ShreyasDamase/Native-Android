# RecyclerView, DiffUtil, and Paging 3

This note covers the classic Android list stack that still appears heavily in interviews and production apps:

- RecyclerView
- DiffUtil / ListAdapter
- Paging 3

Compose is modern, but RecyclerView is still everywhere in legacy and mixed codebases.

---

## 1. RecyclerView Basics

`RecyclerView` is a high-performance list container that reuses item views instead of inflating a new view for every row.

Core pieces:

- `RecyclerView`
- `Adapter`
- `ViewHolder`
- `LayoutManager`

### Why Recycling Matters

If 1,000 items are visible in theory but only 10 are on screen, Android only keeps a small number of view objects alive and rebinds them as you scroll.

That is the whole point of the ViewHolder pattern.

---

## 2. Adapter and ViewHolder

```kotlin
class MessageAdapter(
    private val items: MutableList<Message>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleView)
        private val bodyView: TextView = itemView.findViewById(R.id.bodyView)

        fun bind(item: Message) {
            titleView.text = item.title
            bodyView.text = item.body
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
```

### Key Methods

- `onCreateViewHolder()`: inflate row view
- `onBindViewHolder()`: bind data into an existing holder
- `getItemCount()`: total list size
- `getItemViewType()`: support different row layouts

---

## 3. LayoutManager

The `LayoutManager` decides how items are arranged:

- `LinearLayoutManager`
- `GridLayoutManager`
- `StaggeredGridLayoutManager`

```kotlin
recyclerView.layoutManager = LinearLayoutManager(context)
recyclerView.adapter = messageAdapter
```

---

## 4. Why `notifyDataSetChanged()` Is Bad

`notifyDataSetChanged()` tells RecyclerView:

- everything changed
- rebind all visible rows
- lose fine-grained animations
- do more work than necessary

That is why `DiffUtil` exists.

---

## 5. DiffUtil

`DiffUtil` calculates the minimal set of updates between an old list and a new list.

It answers two important questions:

- Are these two items the same logical item?
- Did the contents of that item change?

### `DiffUtil.ItemCallback`

```kotlin
data class Message(
    val id: String,
    val title: String,
    val body: String
)

object MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}
```

### Rule of Thumb

- `areItemsTheSame`: compare stable identity like database ID
- `areContentsTheSame`: compare visible contents

If you get these wrong, you get bad animations or stale UI.

---

## 6. ListAdapter

`ListAdapter` wraps RecyclerView.Adapter and runs `DiffUtil` on a background thread for you.

```kotlin
class MessageListAdapter :
    ListAdapter<Message, MessageListAdapter.MessageViewHolder>(MessageDiffCallback) {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleView)
        private val bodyView: TextView = itemView.findViewById(R.id.bodyView)

        fun bind(item: Message) {
            titleView.text = item.title
            bodyView.text = item.body
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
```

Submit updates like this:

```kotlin
adapter.submitList(newMessages)
```

### Why This Is Better

- simpler adapter code
- background diff calculation
- nice insert/remove/move animations

---

## 7. Multiple View Types

Useful for chat apps, feed ads, loading rows, headers, and error rows.

```kotlin
override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
        is FeedItem.Article -> VIEW_TYPE_ARTICLE
        is FeedItem.Ad -> VIEW_TYPE_AD
    }
}
```

---

## 8. RecyclerView Performance Tips

- Use `ListAdapter` or `DiffUtil`
- Keep `onBindViewHolder()` cheap
- Avoid allocating objects repeatedly in bind
- Load images with Coil or Glide, not manually
- Use stable IDs only if you truly understand the tradeoffs
- Share `RecycledViewPool` when multiple nested lists use the same item types

### Common Smooth-Scrolling Problems

- expensive image decoding on main thread
- nested RecyclerViews without tuning
- calling `notifyDataSetChanged()` too often
- doing formatting or sorting inside `onBindViewHolder()`

---

## 9. Paging 3

Paging 3 loads large datasets in chunks so the app does not fetch or render everything at once.

Core pieces:

- `PagingSource<Key, Value>`
- `Pager`
- `PagingData`
- `PagingDataAdapter`
- `RemoteMediator` for network + Room

---

## 10. `PagingSource`

```kotlin
class ArticlePagingSource(
    private val api: NewsApi
) : PagingSource<Int, Article>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val page = params.key ?: 1

        return try {
            val response = api.getArticles(page = page, pageSize = params.loadSize)
            val articles = response.articles

            LoadResult.Page(
                data = articles,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (articles.isEmpty()) null else page + 1
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Article>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}
```

### What `load()` Returns

- `LoadResult.Page`
- `LoadResult.Error`

---

## 11. Building the Pager

```kotlin
class ArticleRepository(
    private val api: NewsApi
) {
    fun getPagedArticles(): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                initialLoadSize = 40,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArticlePagingSource(api) }
        ).flow
    }
}
```

In ViewModel:

```kotlin
class ArticleViewModel(
    private val repository: ArticleRepository
) : ViewModel() {

    val articles: Flow<PagingData<Article>> =
        repository.getPagedArticles().cachedIn(viewModelScope)
}
```

Why `cachedIn(viewModelScope)` matters:

- survives configuration changes
- avoids restarting paging on every recollect

---

## 12. `PagingDataAdapter`

```kotlin
class ArticlePagingAdapter :
    PagingDataAdapter<Article, ArticlePagingAdapter.ArticleViewHolder>(ArticleDiffCallback) {

    class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.titleView)

        fun bind(item: Article?) {
            titleView.text = item?.title ?: "Loading..."
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
```

Collect in UI:

```kotlin
lifecycleScope.launch {
    viewModel.articles.collectLatest { pagingData ->
        adapter.submitData(pagingData)
    }
}
```

---

## 13. Load States

Paging exposes loading, error, and idle states.

```kotlin
lifecycleScope.launch {
    adapter.loadStateFlow.collectLatest { loadStates ->
        progressBar.isVisible = loadStates.refresh is LoadState.Loading
        retryButton.isVisible = loadStates.refresh is LoadState.Error
    }
}
```

Important states:

- `refresh`
- `prepend`
- `append`

---

## 14. `RemoteMediator`

Use `RemoteMediator` when:

- the network is not your source of truth
- Room is your source of truth
- you want offline-first paging

Flow:

1. UI reads paged data from Room
2. `RemoteMediator` fetches network pages
3. Save them into Room
4. Room invalidates and UI updates automatically

This is the production-grade approach for feeds, news lists, and catalog screens.

---

## 15. RecyclerView vs Compose Lists

RecyclerView concepts map nicely to Compose:

| RecyclerView | Compose |
| --- | --- |
| `RecyclerView` | `LazyColumn` / `LazyRow` |
| `ViewHolder` | item lambda |
| `DiffUtil` | stable keys + state model discipline |
| `PagingDataAdapter` | `LazyPagingItems` |

Even in Compose interviews, RecyclerView is still expected knowledge.

---

## 16. Interview Answers in One Breath

### How does RecyclerView work?

RecyclerView reuses a small set of view holders and rebinds them with new data while scrolling, which reduces inflation cost and memory usage.

### Why use DiffUtil?

It computes minimal list changes instead of forcing a full redraw, which improves performance and animations.

### Why use Paging 3?

It loads large datasets incrementally, exposes load states, integrates with Room and network layers, and avoids memory and UI overhead from loading everything at once.

---

## Common Mistakes

- calling `notifyDataSetChanged()` for every update
- using position as identity in `areItemsTheSame()`
- doing expensive formatting inside `onBindViewHolder()`
- forgetting `cachedIn(viewModelScope)`
- using network-only paging when the app really needs Room as source of truth
