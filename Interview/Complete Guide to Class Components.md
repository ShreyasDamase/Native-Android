# React Native: Complete Guide to Class Components

## From Hooks to Class Components - A Practical Learning Path

---

## Chapter 1: Understanding State Management

### 1.1 useState → this.state & this.setState

**What You Know (Functional):**

```javascript
function Counter() {
  const [count, setCount] = useState(0);
  const [name, setName] = useState('');
  
  const increment = () => setCount(count + 1);
  const updateName = (text) => setName(text);
}
```

**Class Component Equivalent:**

```javascript
class Counter extends Component {
  constructor(props) {
    super(props);
    // ALL state in ONE object
    this.state = {
      count: 0,
      name: ''
    };
  }
  
  increment = () => {
    // Update only count, name stays unchanged
    this.setState({ count: this.state.count + 1 });
  }
  
  updateName = (text) => {
    this.setState({ name: text });
  }
}
```

**Key Differences:**

|Functional|Class|
|---|---|
|Multiple `useState` calls|One `this.state` object|
|`setCount(newValue)`|`this.setState({ count: newValue })`|
|Direct access: `count`|Must use: `this.state.count`|
|Replaces entire value|Merges with existing state|

**Important: State Merging**

```javascript
// Functional - REPLACES entire state
const [user, setUser] = useState({ name: 'John', age: 25 });
setUser({ name: 'Jane' }); // ❌ age is LOST!

// Class - MERGES with existing state
this.state = { name: 'John', age: 25 };
this.setState({ name: 'Jane' }); // ✅ age is still 25!
```

---

### 1.2 Async State Updates

**Problem: State Updates are Asynchronous**

```javascript
class Counter extends Component {
  state = { count: 0 };
  
  // ❌ WRONG - Won't work as expected!
  incrementThreeTimes = () => {
    this.setState({ count: this.state.count + 1 }); // count = 1
    this.setState({ count: this.state.count + 1 }); // count = 1 (not 2!)
    this.setState({ count: this.state.count + 1 }); // count = 1 (not 3!)
    // Final result: count = 1 (not 3!)
  }
  
  // ✅ CORRECT - Use functional form
  incrementThreeTimes = () => {
    this.setState(prevState => ({ count: prevState.count + 1 }));
    this.setState(prevState => ({ count: prevState.count + 1 }));
    this.setState(prevState => ({ count: prevState.count + 1 }));
    // Final result: count = 3 ✅
  }
}
```

**Rule:** When new state depends on previous state, always use functional form.

---

### 1.3 setState Callback

**Functional:**

```javascript
const [loading, setLoading] = useState(false);

const fetchData = async () => {
  setLoading(true);
  await api.getData();
  setLoading(false);
  // Want to do something AFTER state updates?
  // Need useEffect with loading dependency
};
```

**Class:**

```javascript
fetchData = async () => {
  this.setState({ loading: true }, () => {
    // This callback runs AFTER state updates!
    console.log('Loading state updated:', this.state.loading);
  });
  
  await api.getData();
  this.setState({ loading: false });
}
```

---

## Chapter 2: Effect Management

### 2.1 useEffect(() => {}, []) → componentDidMount

**When:** Run code once when component appears

**Functional:**

```javascript
useEffect(() => {
  fetchUserData();
  setupSubscriptions();
}, []);
```

**Class:**

```javascript
componentDidMount() {
  this.fetchUserData();
  this.setupSubscriptions();
}
```

**Real Example: Data Fetching on Load**

```javascript
class ProfileScreen extends Component {
  state = {
    user: null,
    loading: true,
    error: null
  };
  
  componentDidMount() {
    this.loadUserProfile();
  }
  
  loadUserProfile = async () => {
    try {
      const response = await fetch('/api/user/profile');
      const user = await response.json();
      this.setState({ user, loading: false });
    } catch (error) {
      this.setState({ error: error.message, loading: false });
    }
  }
  
  render() {
    const { user, loading, error } = this.state;
    
    if (loading) return <ActivityIndicator />;
    if (error) return <Text>{error}</Text>;
    
    return (
      <View>
        <Text>{user.name}</Text>
        <Text>{user.email}</Text>
      </View>
    );
  }
}
```

---

### 2.2 useEffect(() => {}, [dependency]) → componentDidUpdate

**When:** Run code when specific values change

**Functional:**

```javascript
useEffect(() => {
  searchProducts(query);
}, [query]); // Runs when query changes
```

**Class:**

```javascript
componentDidUpdate(prevProps, prevState) {
  // MUST compare prev values to avoid infinite loop!
  if (prevState.query !== this.state.query) {
    this.searchProducts(this.state.query);
  }
}
```

**Real Example: Search as You Type**

```javascript
class SearchScreen extends Component {
  state = {
    query: '',
    results: [],
    loading: false
  };
  
  componentDidUpdate(prevProps, prevState) {
    // Only search when query actually changed
    if (prevState.query !== this.state.query) {
      this.performSearch();
    }
  }
  
  performSearch = async () => {
    const { query } = this.state;
    
    if (query.length < 3) {
      this.setState({ results: [] });
      return;
    }
    
    this.setState({ loading: true });
    
    try {
      const response = await fetch(`/api/search?q=${query}`);
      const results = await response.json();
      this.setState({ results, loading: false });
    } catch (error) {
      this.setState({ loading: false });
    }
  }
  
  render() {
    return (
      <View>
        <TextInput
          value={this.state.query}
          onChangeText={(text) => this.setState({ query: text })}
          placeholder="Search..."
        />
        {this.state.loading ? (
          <ActivityIndicator />
        ) : (
          <FlatList data={this.state.results} />
        )}
      </View>
    );
  }
}
```

**Common Patterns:**

```javascript
componentDidUpdate(prevProps, prevState) {
  // Pattern 1: Props changed
  if (prevProps.userId !== this.props.userId) {
    this.loadUserData(this.props.userId);
  }
  
  // Pattern 2: Multiple conditions
  if (prevState.filter !== this.state.filter || 
      prevState.sortBy !== this.state.sortBy) {
    this.refreshData();
  }
  
  // Pattern 3: State crossed threshold
  if (prevState.items.length < 10 && this.state.items.length >= 10) {
    this.showCompletionMessage();
  }
}
```

---

### 2.3 useEffect Cleanup → componentWillUnmount

**When:** Clean up before component disappears

**Functional:**

```javascript
useEffect(() => {
  const subscription = setupListener();
  
  return () => {
    subscription.unsubscribe(); // Cleanup
  };
}, []);
```

**Class:**

```javascript
componentDidMount() {
  this.subscription = this.setupListener();
}

componentWillUnmount() {
  this.subscription.unsubscribe(); // Cleanup
}
```

**Real Example: Location Tracking**

```javascript
class MapScreen extends Component {
  state = {
    location: null
  };
  
  componentDidMount() {
    // Start watching location
    this.watchId = Geolocation.watchPosition(
      (position) => {
        this.setState({
          location: {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          }
        });
      },
      (error) => console.log(error),
      { enableHighAccuracy: true, distanceFilter: 10 }
    );
    
    // Setup back button handler
    this.backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      this.handleBackPress
    );
  }
  
  componentWillUnmount() {
    // CRITICAL: Stop watching location
    if (this.watchId !== null) {
      Geolocation.clearWatch(this.watchId);
    }
    
    // Remove back button listener
    this.backHandler.remove();
  }
  
  handleBackPress = () => {
    // Handle back navigation
    return true;
  }
  
  render() {
    // Render map...
  }
}
```

**Common Cleanup Scenarios:**

```javascript
componentWillUnmount() {
  // 1. Clear timers
  if (this.timer) {
    clearInterval(this.timer);
    clearTimeout(this.timeout);
  }
  
  // 2. Remove event listeners
  this.subscription.remove();
  DeviceEventEmitter.removeAllListeners('customEvent');
  
  // 3. Cancel network requests
  if (this.abortController) {
    this.abortController.abort();
  }
  
  // 4. Clear animations
  if (this.animation) {
    this.animation.stop();
  }
  
  // 5. Unsubscribe from stores
  if (this.unsubscribe) {
    this.unsubscribe();
  }
}
```

---

## Chapter 3: Advanced Hooks Conversion

### 3.1 useCallback → Class Methods

**What useCallback Does:** Memoizes functions to prevent recreation

**Functional:**

```javascript
const handlePress = useCallback(() => {
  doSomething(id);
}, [id]); // Only recreate when id changes
```

**Class - Automatic Memoization:**

```javascript
class MyComponent extends Component {
  // Arrow function = automatically bound, never recreated
  handlePress = () => {
    this.doSomething(this.props.id);
  }
  
  // Regular function - need to bind in constructor
  handlePress() {
    this.doSomething(this.props.id);
  }
}
```

**Why Class Components Don't Need useCallback:**

- Class methods are defined once on the prototype
- Arrow function properties are created once per instance
- They don't recreate on every render like functional components

**Real Example: Passing Callbacks to Children**

```javascript
class TodoList extends Component {
  state = {
    todos: []
  };
  
  // This function reference NEVER changes
  toggleTodo = (id) => {
    this.setState(prevState => ({
      todos: prevState.todos.map(todo =>
        todo.id === id ? { ...todo, completed: !todo.completed } : todo
      )
    }));
  }
  
  // This function reference NEVER changes
  deleteTodo = (id) => {
    this.setState(prevState => ({
      todos: prevState.todos.filter(todo => todo.id !== id)
    }));
  }
  
  render() {
    return (
      <FlatList
        data={this.state.todos}
        renderItem={({ item }) => (
          // Same function reference every render - no unnecessary re-renders!
          <TodoItem
            todo={item}
            onToggle={this.toggleTodo}
            onDelete={this.deleteTodo}
          />
        )}
      />
    );
  }
}
```

---

### 3.2 useMemo → Computed Values

**What useMemo Does:** Memoizes expensive calculations

**Functional:**

```javascript
const sortedData = useMemo(() => {
  return data.sort((a, b) => a.name.localeCompare(b.name));
}, [data]);
```

**Class - Calculate in Render:**

```javascript
class DataList extends Component {
  render() {
    // Option 1: Calculate every render (if cheap)
    const sortedData = this.props.data.sort((a, b) => 
      a.name.localeCompare(b.name)
    );
    
    return <FlatList data={sortedData} />;
  }
}
```

**Class - Manual Memoization (if expensive):**

```javascript
class DataList extends Component {
  constructor(props) {
    super(props);
    this.cachedSort = null;
    this.lastData = null;
  }
  
  getSortedData() {
    // Only recalculate if data changed
    if (this.props.data !== this.lastData) {
      this.cachedSort = this.props.data.sort((a, b) => 
        a.name.localeCompare(b.name)
      );
      this.lastData = this.props.data;
    }
    return this.cachedSort;
  }
  
  render() {
    const sortedData = this.getSortedData();
    return <FlatList data={sortedData} />;
  }
}
```

**Real Example: Filtering & Sorting**

```javascript
class ProductList extends Component {
  state = {
    searchQuery: '',
    sortBy: 'name',
    categoryFilter: 'all'
  };
  
  getFilteredProducts() {
    const { products } = this.props;
    const { searchQuery, categoryFilter, sortBy } = this.state;
    
    // Filter by search
    let filtered = products.filter(p =>
      p.name.toLowerCase().includes(searchQuery.toLowerCase())
    );
    
    // Filter by category
    if (categoryFilter !== 'all') {
      filtered = filtered.filter(p => p.category === categoryFilter);
    }
    
    // Sort
    filtered.sort((a, b) => {
      if (sortBy === 'name') {
        return a.name.localeCompare(b.name);
      } else if (sortBy === 'price') {
        return a.price - b.price;
      }
      return 0;
    });
    
    return filtered;
  }
  
  render() {
    const products = this.getFilteredProducts();
    
    return (
      <View>
        <TextInput
          value={this.state.searchQuery}
          onChangeText={(text) => this.setState({ searchQuery: text })}
        />
        <FlatList data={products} />
      </View>
    );
  }
}
```

---

### 3.3 useRef → createRef & Instance Variables

**What useRef Does:** Stores mutable values that persist across renders

**Functional:**

```javascript
function MyComponent() {
  const inputRef = useRef(null); // For DOM reference
  const countRef = useRef(0); // For mutable value
  
  const focusInput = () => {
    inputRef.current.focus();
  };
  
  const incrementSilently = () => {
    countRef.current += 1; // No re-render!
  };
}
```

**Class Equivalent:**

```javascript
class MyComponent extends Component {
  // For DOM references - use createRef
  inputRef = React.createRef();
  
  // For mutable values - use instance variables
  count = 0;
  timer = null;
  subscription = null;
  
  focusInput = () => {
    this.inputRef.current.focus();
  }
  
  incrementSilently = () => {
    this.count += 1; // No re-render!
  }
  
  componentDidMount() {
    // Store timer reference
    this.timer = setInterval(() => {
      this.count += 1;
    }, 1000);
  }
  
  componentWillUnmount() {
    // Clean up using stored reference
    clearInterval(this.timer);
  }
  
  render() {
    return (
      <TextInput
        ref={this.inputRef}
        onSubmitEditing={this.focusInput}
      />
    );
  }
}
```

**Real Example: Form with Multiple Inputs**

```javascript
class LoginForm extends Component {
  // Create refs for each input
  emailRef = React.createRef();
  passwordRef = React.createRef();
  
  state = {
    email: '',
    password: ''
  };
  
  handleEmailSubmit = () => {
    // Move to next input
    this.passwordRef.current.focus();
  }
  
  handlePasswordSubmit = () => {
    this.handleLogin();
  }
  
  handleLogin = () => {
    const { email, password } = this.state;
    // Perform login
  }
  
  render() {
    return (
      <View>
        <TextInput
          ref={this.emailRef}
          value={this.state.email}
          onChangeText={(text) => this.setState({ email: text })}
          onSubmitEditing={this.handleEmailSubmit}
          returnKeyType="next"
        />
        <TextInput
          ref={this.passwordRef}
          value={this.state.password}
          onChangeText={(text) => this.setState({ password: text })}
          onSubmitEditing={this.handlePasswordSubmit}
          returnKeyType="done"
          secureTextEntry
        />
      </View>
    );
  }
}
```

**Real Example: Tracking Previous Values**

```javascript
class Counter extends Component {
  state = {
    count: 0
  };
  
  // Store previous count
  previousCount = this.state.count;
  
  componentDidUpdate() {
    // Update previous value AFTER render
    this.previousCount = this.state.count;
  }
  
  render() {
    return (
      <View>
        <Text>Current: {this.state.count}</Text>
        <Text>Previous: {this.previousCount}</Text>
        <Button
          title="Increment"
          onPress={() => this.setState({ count: this.state.count + 1 })}
        />
      </View>
    );
  }
}
```

---

### 3.4 useContext → Context.Consumer or contextType

**Setting Up Context (Same for Both):**

```javascript
// ThemeContext.js
import { createContext } from 'react';

export const ThemeContext = createContext({
  theme: 'light',
  toggleTheme: () => {}
});
```

**Functional:**

```javascript
function ThemedButton() {
  const { theme, toggleTheme } = useContext(ThemeContext);
  
  return (
    <Button
      title="Toggle Theme"
      onPress={toggleTheme}
      color={theme === 'dark' ? '#fff' : '#000'}
    />
  );
}
```

**Class - Method 1: contextType (Simpler)**

```javascript
class ThemedButton extends Component {
  // Magic property name!
  static contextType = ThemeContext;
  
  render() {
    // Access context via this.context
    const { theme, toggleTheme } = this.context;
    
    return (
      <Button
        title="Toggle Theme"
        onPress={toggleTheme}
        color={theme === 'dark' ? '#fff' : '#000'}
      />
    );
  }
}
```

**Class - Method 2: Context.Consumer (For Multiple Contexts)**

```javascript
class ThemedButton extends Component {
  render() {
    return (
      <ThemeContext.Consumer>
        {({ theme, toggleTheme }) => (
          <Button
            title="Toggle Theme"
            onPress={toggleTheme}
            color={theme === 'dark' ? '#fff' : '#000'}
          />
        )}
      </ThemeContext.Consumer>
    );
  }
}
```

**Real Example: Complete Theme System**

```javascript
// App.js
class App extends Component {
  state = {
    theme: 'light'
  };
  
  toggleTheme = () => {
    this.setState(prevState => ({
      theme: prevState.theme === 'light' ? 'dark' : 'light'
    }));
  }
  
  render() {
    const contextValue = {
      theme: this.state.theme,
      toggleTheme: this.toggleTheme
    };
    
    return (
      <ThemeContext.Provider value={contextValue}>
        <NavigationContainer>
          <MainNavigator />
        </NavigationContainer>
      </ThemeContext.Provider>
    );
  }
}

// Screen.js
class ProfileScreen extends Component {
  static contextType = ThemeContext;
  
  render() {
    const { theme } = this.context;
    const isDark = theme === 'dark';
    
    return (
      <View style={{
        flex: 1,
        backgroundColor: isDark ? '#000' : '#fff'
      }}>
        <Text style={{ color: isDark ? '#fff' : '#000' }}>
          Profile Screen
        </Text>
      </View>
    );
  }
}
```

**Using Multiple Contexts:**

```javascript
class UserProfile extends Component {
  render() {
    return (
      <ThemeContext.Consumer>
        {({ theme }) => (
          <UserContext.Consumer>
            {({ user, updateUser }) => (
              <LanguageContext.Consumer>
                {({ language, changeLanguage }) => (
                  <View>
                    <Text style={{ color: theme === 'dark' ? '#fff' : '#000' }}>
                      {language === 'en' ? user.name : user.nameLocal}
                    </Text>
                  </View>
                )}
              </LanguageContext.Consumer>
            )}
          </UserContext.Consumer>
        )}
      </ThemeContext.Consumer>
    );
  }
}
```

---

## Chapter 4: React Navigation with Class Components

### 4.1 useFocusEffect → Navigation Listeners

**What useFocusEffect Does:** Run code when screen comes into focus

**Functional:**

```javascript
import { useFocusEffect } from '@react-navigation/native';

function ProfileScreen() {
  useFocusEffect(
    useCallback(() => {
      fetchLatestData();
      
      return () => {
        // Cleanup when leaving screen
      };
    }, [])
  );
}
```

**Class - Using Navigation Listeners:**

```javascript
class ProfileScreen extends Component {
  componentDidMount() {
    // Add listener when component mounts
    this.focusListener = this.props.navigation.addListener('focus', () => {
      this.fetchLatestData();
    });
    
    this.blurListener = this.props.navigation.addListener('blur', () => {
      // Cleanup when leaving screen
      this.cleanup();
    });
  }
  
  componentWillUnmount() {
    // Remove listeners
    if (this.focusListener) {
      this.focusListener();
    }
    if (this.blurListener) {
      this.blurListener();
    }
  }
  
  fetchLatestData = () => {
    // Refresh data every time screen comes into focus
  }
  
  cleanup = () => {
    // Stop any ongoing operations
  }
  
  render() {
    return <View>...</View>;
  }
}
```

**Real Example: Chat Screen That Refreshes**

```javascript
class ChatScreen extends Component {
  state = {
    messages: [],
    loading: false
  };
  
  componentDidMount() {
    // Initial load
    this.loadMessages();
    
    // Refresh when returning to screen
    this.focusListener = this.props.navigation.addListener('focus', () => {
      this.loadMessages();
      this.markMessagesAsRead();
    });
    
    // Stop polling when leaving
    this.blurListener = this.props.navigation.addListener('blur', () => {
      this.stopPolling();
    });
  }
  
  componentWillUnmount() {
    this.focusListener?.();
    this.blurListener?.();
    this.stopPolling();
  }
  
  loadMessages = async () => {
    this.setState({ loading: true });
    const messages = await fetchMessages(this.props.route.params.chatId);
    this.setState({ messages, loading: false });
    this.startPolling();
  }
  
  startPolling = () => {
    this.pollingTimer = setInterval(() => {
      this.loadMessages();
    }, 5000);
  }
  
  stopPolling = () => {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
  }
  
  markMessagesAsRead = async () => {
    await markAsRead(this.props.route.params.chatId);
  }
  
  render() {
    // Render messages...
  }
}
```

---

### 4.2 useNavigation → this.props.navigation

**Functional:**

```javascript
import { useNavigation } from '@react-navigation/native';

function MyButton() {
  const navigation = useNavigation();
  
  return (
    <Button
      title="Go to Profile"
      onPress={() => navigation.navigate('Profile', { userId: 123 })}
    />
  );
}
```

**Class - Navigation via Props:**

```javascript
// Direct child of navigator - has navigation prop automatically
class HomeScreen extends Component {
  goToProfile = () => {
    this.props.navigation.navigate('Profile', { userId: 123 });
  }
  
  render() {
    return (
      <View>
        <Button title="Go to Profile" onPress={this.goToProfile} />
        
        {/* Pass to child components */}
        <MyButton navigation={this.props.navigation} />
      </View>
    );
  }
}

// Child component - receives navigation via props
class MyButton extends Component {
  render() {
    return (
      <Button
        title="Go to Profile"
        onPress={() => 
          this.props.navigation.navigate('Profile', { userId: 123 })
        }
      />
    );
  }
}
```

**Alternative: withNavigation HOC (Deprecated but useful)**

```javascript
import { withNavigation } from '@react-navigation/native';

class MyButton extends Component {
  // Now has this.props.navigation!
  render() {
    return (
      <Button
        title="Go to Profile"
        onPress={() => 
          this.props.navigation.navigate('Profile', { userId: 123 })
        }
      />
    );
  }
}

export default withNavigation(MyButton);
```

---

### 4.3 useRoute → this.props.route

**Functional:**

```javascript
import { useRoute } from '@react-navigation/native';

function ProfileScreen() {
  const route = useRoute();
  const { userId, userName } = route.params;
  
  return <Text>{userName}</Text>;
}
```

**Class:**

```javascript
class ProfileScreen extends Component {
  render() {
    // Access route params
    const { userId, userName } = this.props.route.params;
    
    return <Text>{userName}</Text>;
  }
}
```

**Complete Navigation Example:**

```javascript
class ProfileScreen extends Component {
  state = {
    user: null,
    loading: true
  };
  
  componentDidMount() {
    // Get params from route
    const { userId } = this.props.route.params;
    this.loadUser(userId);
    
    // Set navigation options dynamically
    this.props.navigation.setOptions({
      title: 'Loading...'
    });
  }
  
  loadUser = async (userId) => {
    try {
      const user = await fetchUser(userId);
      this.setState({ user, loading: false });
      
      // Update title with user name
      this.props.navigation.setOptions({
        title: user.name
      });
    } catch (error) {
      this.setState({ loading: false });
    }
  }
  
  goToSettings = () => {
    this.props.navigation.navigate('Settings', {
      userId: this.state.user.id
    });
  }
  
  goBack = () => {
    this.props.navigation.goBack();
  }
  
  render() {
    const { user, loading } = this.state;
    
    if (loading) {
      return <ActivityIndicator />;
    }
    
    return (
      <View>
        <Text>{user.name}</Text>
        <Button title="Settings" onPress={this.goToSettings} />
        <Button title="Go Back" onPress={this.goBack} />
      </View>
    );
  }
}
```

---

## Chapter 5: Performance Optimization

### 5.1 React.memo → shouldComponentUpdate

**What React.memo Does:** Prevents re-render if props haven't changed

**Functional:**

```javascript
const UserCard = React.memo(({ user }) => {
  return (
    <View>
      <Text>{user.name}</Text>
    </View>
  );
});
```

**Class - shouldComponentUpdate:**

```javascript
class UserCard extends Component {
  shouldComponentUpdate(nextProps, nextState) {
    // Only re-render if user prop changed
    return nextProps.user.id !== this.props.user.id;
  }
  
  render() {
    const { user } = this.props;
    return (
      <View>
        <Text>{user.name}</Text>
      </View>
    );
  }
}
```

**Class - PureComponent (Automatic Shallow Comparison):**

```javascript
import { PureComponent } from 'react';

class UserCard extends PureComponent {
  // Automatically does shallow comparison of props and state!
  // No need to write shouldComponentUpdate
  
  render() {
    const { user } = this.props;
    return (
      <View>
        <Text>{user.name}</Text>
      </View>
    );
  }
}
```

**Real Example: Optimized List Item**

```javascript
class TodoItem extends PureComponent {
  handleToggle = () => {
    this.props.onToggle(this.props.todo.id);
  }
  
  handleDelete = () => {
    this.props.onDelete(this.props.todo.id);
  }
  
  render() {
    const { todo } = this.props;
    console.log('TodoItem rendered:', todo.id); // See when it renders
    
    return (
      <View style={styles.item}>
        <TouchableOpacity onPress={this.handleToggle}>
          <Text style={todo.completed && styles.completed}>
            {todo.text}
          </Text>
        </TouchableOpacity>
        <Button title="Delete" onPress={this.handleDelete} />
      </View>
    );
  }
}

class TodoList extends Component {
  state = {
    todos: []
  };
  
  toggleTodo = (id) => {
    this.setState(prevState => ({
      todos: prevState.todos.map(todo =>
        todo.id === id ? { ...todo, completed: !todo.completed } : todo
      )
    }));
  }
  
  render() {
    return (
      <FlatList
        data={this.state.todos}
        renderItem={({ item }) => (
          <TodoItem
            todo={item}
            onToggle={this.toggleTodo}
            onDelete={this.deleteTodo}
          />
        )}
        keyExtractor={item => item.id}
      />
    );
  }
}
```

**When to Use Each:**

- **PureComponent:** Most cases - automatic optimization
- **shouldComponentUpdate:** Complex props, custom comparison logic
- **Component
:** Rare cases where you want to control re-renders manually

---

### 5.2 Advanced shouldComponentUpdate

**Custom Comparison Logic:**

```javascript
class ProductCard extends Component {
  shouldComponentUpdate(nextProps, nextState) {
    // Multiple conditions
    const propsChanged = 
      nextProps.product.id !== this.props.product.id ||
      nextProps.product.price !== this.props.product.price ||
      nextProps.isSelected !== this.props.isSelected;
    
    const stateChanged = 
      nextState.quantity !== this.state.quantity;
    
    return propsChanged || stateChanged;
  }
  
  render() {
    // Expensive render...
  }
}
```

**Deep Comparison (Be Careful - Expensive!):**

```javascript
import _ from 'lodash';

class DataTable extends Component {
  shouldComponentUpdate(nextProps) {
    // Use lodash for deep comparison
    return !_.isEqual(this.props.data, nextProps.data);
  }
  
  render() {
    // Render large table...
  }
}
```

---

## Chapter 6: Complete Real-World App Example

Let me create a complete Todo App with ALL concepts:---

## Chapter 7: Common Patterns & Best Practices

### 7.1 Method Binding - The 'this' Problem

**The Problem:**

```javascript
class MyComponent extends Component {
  state = { count: 0 };
  
  // ❌ WRONG - 'this' will be undefined!
  handleClick() {
    this.setState({ count: this.state.count + 1 });
  }
  
  render() {
    return <button onClick={this.handleClick}>Click</button>;
  }
}
```

**Solution 1: Arrow Function (Recommended)**

```javascript
class MyComponent extends Component {
  // ✅ Arrow function = automatically bound
  handleClick = () => {
    this.setState({ count: this.state.count + 1 });
  }
}
```

**Solution 2: Bind in Constructor**

```javascript
class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.handleClick = this.handleClick.bind(this);
  }
  
  handleClick() {
    this.setState({ count: this.state.count + 1 });
  }
}
```

**Solution 3: Inline Arrow Function (Not Recommended - Creates New Function Each Render)**

```javascript
render() {
  return (
    <button onClick={() => this.handleClick()}>
      Click
    </button>
  );
}
```

---

### 7.2 Passing Data Between Components

**Parent to Child (Props):**

```javascript
class ParentScreen extends Component {
  state = { userName: 'John' };
  
  render() {
    return (
      <ChildComponent 
        name={this.state.userName}
        age={25}
        onUpdate={this.handleUpdate}
      />
    );
  }
}

class ChildComponent extends Component {
  render() {
    return (
      <View>
        <Text>{this.props.name}</Text>
        <Text>{this.props.age}</Text>
      </View>
    );
  }
}
```

**Child to Parent (Callbacks):**

```javascript
class ParentScreen extends Component {
  state = { selectedItem: null };
  
  handleItemSelect = (item) => {
    this.setState({ selectedItem: item });
  }
  
  render() {
    return (
      <ItemList onItemSelect={this.handleItemSelect} />
    );
  }
}

class ItemList extends Component {
  render() {
    return (
      <TouchableOpacity 
        onPress={() => this.props.onItemSelect(item)}
      >
        <Text>{item.name}</Text>
      </TouchableOpacity>
    );
  }
}
```

---

### 7.3 Error Boundaries (Only in Class Components!)

**No Hook Equivalent - Must Use Class:**

```javascript
class ErrorBoundary extends Component {
  state = { hasError: false, error: null };
  
  static getDerivedStateFromError(error) {
    // Update state to show fallback UI
    return { hasError: true, error };
  }
  
  componentDidCatch(error, errorInfo) {
    // Log error to reporting service
    console.error('Error caught:', error, errorInfo);
    // logErrorToService(error, errorInfo);
  }
  
  render() {
    if (this.state.hasError) {
      return (
        <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
          <Text style={{ fontSize: 20, marginBottom: 10 }}>
            Something went wrong! 😕
          </Text>
          <Text style={{ color: '#666', marginBottom: 20 }}>
            {this.state.error?.message}
          </Text>
          <Button
            title="Try Again"
            onPress={() => this.setState({ hasError: false, error: null })}
          />
        </View>
      );
    }
    
    return this.props.children;
  }
}

// Usage:
class App extends Component {
  render() {
    return (
      <ErrorBoundary>
        <MainNavigator />
      </ErrorBoundary>
    );
  }
}
```

---

### 7.4 Conditional Rendering Patterns

```javascript
class ConditionalScreen extends Component {
  state = {
    loading: true,
    error: null,
    data: null
  };
  
  render() {
    const { loading, error, data } = this.state;
    
    // Pattern 1: Early returns
    if (loading) {
      return <ActivityIndicator size="large" />;
    }
    
    if (error) {
      return (
        <View>
          <Text>Error: {error}</Text>
          <Button title="Retry" onPress={this.retry} />
        </View>
      );
    }
    
    if (!data) {
      return <Text>No data available</Text>;
    }
    
    // Pattern 2: Ternary operator
    return (
      <View>
        {data.length > 0 ? (
          <FlatList data={data} />
        ) : (
          <Text>No items</Text>
        )}
      </View>
    );
    
    // Pattern 3: Logical AND
    return (
      <View>
        {data && <Text>{data.title}</Text>}
        {data?.items?.length > 0 && <ItemList items={data.items} />}
      </View>
    );
  }
}
```

---

## Chapter 8: Testing Class Components

### 8.1 Basic Testing Setup

```javascript
import { render, fireEvent, waitFor } from '@testing-library/react-native';

describe('Counter Component', () => {
  test('increments counter on button press', () => {
    const { getByText, getByTestId } = render(<Counter />);
    
    const button = getByText('Increment');
    const counter = getByTestId('counter-value');
    
    expect(counter.props.children).toBe('0');
    
    fireEvent.press(button);
    
    expect(counter.props.children).toBe('1');
  });
  
  test('loads data on mount', async () => {
    const { getByText } = render(<DataScreen />);
    
    await waitFor(() => {
      expect(getByText('Data loaded')).toBeTruthy();
    });
  });
});
```

---

## Chapter 9: Migration Checklist

**When Converting Functional → Class:**

1. **Setup:**
    
    ```javascript
    // Functional
    function MyScreen() {
    
    // Class
    class MyScreen extends Component {
      render() {
    ```
    
2. **Props:**
    
    ```javascript
    // Functional
    const { userId, name } = props;
    
    // Class
    const { userId, name } = this.props;
    ```
    
3. **State:**
    
    ```javascript
    // Functional
    const [count, setCount] = useState(0);
    setCount(1);
    
    // Class
    state = { count: 0 };
    this.setState({ count: 1 });
    ```
    
4. **Effects:**
    
    ```javascript
    // Functional
    useEffect(() => {
      fetchData();
    }, []);
    
    // Class
    componentDidMount() {
      this.fetchData();
    }
    ```
    
5. **Refs:**
    
    ```javascript
    // Functional
    const inputRef = useRef(null);
    
    // Class
    inputRef = React.createRef();
    ```
    
6. **Context:**
    
    ```javascript
    // Functional
    const theme = useContext(ThemeContext);
    
    // Class
    static contextType = ThemeContext;
    // then: this.context
    ```
    
7. **Callbacks:**
    
    ```javascript
    // Functional
    const handlePress = useCallback(() => {}, []);
    
    // Class
    handlePress = () => {}  // Arrow function
    ```
    

---

## Chapter 10: Interview Questions & Answers

### Q1: Explain the lifecycle of a Class Component

**Answer:** "A Class Component has three main lifecycle phases:

**Mounting** - When component appears:

- `constructor()` - Initialize state
- `render()` - Return JSX
- `componentDidMount()` - After first render, perfect for API calls

**Updating** - When props/state change:

- `render()` - Re-render with new data
- `componentDidUpdate()` - After re-render, compare prev values to run effects

**Unmounting** - When component disappears:

- `componentWillUnmount()` - Cleanup: remove listeners, clear timers, cancel requests

For example, in a profile screen, I'd fetch user data in `componentDidMount`, update it when userId prop changes in `componentDidUpdate`, and stop any subscriptions in `componentWillUnmount`."

---

### Q2: What's the difference between setState being async vs sync?

**Answer:** "`setState` is asynchronous for performance - React batches multiple `setState` calls together. This means you can't rely on `this.state` immediately after calling `setState`.

**Wrong way:**

```javascript
this.setState({ count: this.state.count + 1 });
console.log(this.state.count); // Still old value!
```

**Right way - Functional form:**

```javascript
this.setState(prevState => ({ count: prevState.count + 1 }));
```

**Or use callback:**

```javascript
this.setState({ count: 5 }, () => {
  console.log(this.state.count); // Now it's 5!
});
```

The functional form is essential when new state depends on previous state."

---

### Q3: When would you use PureComponent vs Component?

**Answer:** "`PureComponent` automatically does a shallow comparison of props and state before re-rendering. If nothing changed, it skips the render.

**Use PureComponent when:**

- Component renders same output for same props/state
- Props/state are primitives or shallow objects
- Want easy performance optimization

**Use Component when:**

- Need deep comparison (PureComponent's shallow comparison isn't enough)
- Props are functions that change on every render
- Want full control with custom `shouldComponentUpdate`

For example, a list item component that just displays data is perfect for PureComponent - it only re-renders when the item data actually changes."

---

### Q4: How do you prevent memory leaks in Class Components?

**Answer:** "Memory leaks happen when you forget to clean up subscriptions, timers, or listeners. I always use `componentWillUnmount` for cleanup:

```javascript
componentDidMount() {
  // Setup
  this.timer = setInterval(() => {}, 1000);
  this.subscription = EventEmitter.addListener('event', this.handler);
  this.watchId = Geolocation.watchPosition(this.handler);
}

componentWillUnmount() {
  // Cleanup - CRITICAL!
  clearInterval(this.timer);
  this.subscription.remove();
  Geolocation.clearWatch(this.watchId);
}
```

Common sources of leaks:

- Timers (setInterval, setTimeout)
- Event listeners
- Location watchers
- WebSocket connections
- Subscriptions

The rule is: whatever you set up in `componentDidMount` or `componentDidUpdate`, clean it up in `componentWillUnmount`."

---

### Q5: Explain 'this' binding in Class Components

**Answer:** "In JavaScript classes, methods lose their `this` context when passed as callbacks. There are three solutions:

**Best: Arrow function property**

```javascript
handleClick = () => {
  this.setState({ clicked: true });
}
```

Automatically bound, created once per instance.

**Good: Bind in constructor**

```javascript
constructor(props) {
  super(props);
  this.handleClick = this.handleClick.bind(this);
}
```

**Avoid: Inline arrow function**

```javascript
<Button onPress={() => this.handleClick()} />
```

Creates new function every render - bad for performance.

I always use arrow function properties - they're cleaner and don't need constructor binding."

---

## Final Thoughts

**Why Learn Class Components in 2024?**

1. **Legacy Codebases:** Many production apps still use them
2. **Interviews:** Shows deep React knowledge
3. **Error Boundaries:** Only class components can catch errors
4. **Understanding React:** Helps understand how Hooks work under the hood
5. **Team Flexibility:** Work with any codebase

**Your Learning Path:**

1. ✅ Build the Todo App above
2. ✅ Convert one of your functional apps to class components
3. ✅ Practice lifecycle methods with real scenarios
4. ✅ Understand when to use PureComponent vs Component
5. ✅ Learn Error Boundaries (only class feature!)

**Remember:** While Function Components are the future, Class Components are still valuable knowledge for any React Native developer. They teach you fundamental concepts that make you better at using Hooks too!

---

**Quick Reference Card:**

```
FUNCTIONAL             →  CLASS
─────────────────────────────────────────────
function MyScreen()    →  class MyScreen extends Component
props.name             →  this.props.name
useState(0)            →  state = { count: 0 }
setCount(1)            →  this.setState({ count: 1 })
useEffect(..., [])     →  componentDidMount()
useEffect(..., [x])    →  componentDidUpdate(prev, prevState)
useEffect cleanup      →  componentWillUnmount()
useCallback            →  handleClick = () => {}
useMemo                →  Calculate in render or cache manually
useRef                 →  React.createRef() or this.variable
useContext             →  static contextType or Consumer
React.memo             →  PureComponent or shouldComponentUpdate
```

Good luck with your class component journey! 🚀