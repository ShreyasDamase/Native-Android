
1. Create a state pass function and initial state her is 0   const [state, dispatch] = useReducer(fun, 0);
2. create function fun  with (state: stateProp , action: ActionType )
3. use switch case in fun (){} to case handle 
4. and do state manipulation with parameter state eg. state+1 
5. return  custom hook with {state ,dispatch }
6. interface guide 
- type ActionType = { type: 'increase' } | { type: 'decrease' };
- it is objet of key type 


## how to access it 
```tsx
import { useReducerhook } from './hookPractice/useReducerhook';
const { state, dispatch } = useReducerhook();
<Button title="increase" onPress={() => dispatch({ type: 'increase' })} />
```

always dispatch action as object with type as key value as string  { type: 'increase' }


```tsx
import { useReducer } from 'react';

  

type ActionType = { type: 'increase' } | { type: 'decrease' };

  

export const useReducerhook = () => {

  const [state, dispatch] = useReducer(fun, 0);

  

  function fun(state: number, action: ActionType) {

    switch (action.type) {

      case 'increase':

        return state + 1;

      case 'decrease':

        return state - 1;

      default:

        return state;

    }

  }

  return { state, dispatch };

};
```