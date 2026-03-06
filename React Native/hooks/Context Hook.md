## Steps to create Context hook 

1. Create interface ContextProp for context content type
2. create interface for  child type  as children with  name childrenProp 
3. create context using createContext methd and assign type as union of ContextProp and undefined to const MyContext 
4. create ContextProvider componnent 
- add useStates for to set data and export data 
- and retun MyContext.provider with value={{}} to children 
5. Create useMyContext hook 
 - use useContext() hook to access MyContext context
 - and if it exist return context or else throw new Error with message Use Context must be within provider
 - now add this ContextProvidet to root level
 
## interface guide 
- for setState use ` Dispactch Dispatch<SetStateAction<type>> `;
- for childtern type use ReactNode



```typescript
import {createContext, Dispatch,ReactNode,SetStateAction, useContext,useState,} from 'react';

  

interface ContextType {

  name: string;

  age: number;

  data: any;

  setName: Dispatch<SetStateAction<string>>;

  setAge: Dispatch<SetStateAction<number>>;

  setData: Dispatch<SetStateAction<any>>;

}

  

interface childProp {

  children: ReactNode;

}

const MyContext = createContext<ContextType | undefined>(undefined);

  

export const ContextProvider = ({ children }: childProp) => {

  const [age, setAge] = useState(0);

  const [name, setName] = useState('');

  const [data, setData] = useState({});

  

  return (

    <MyContext.Provider value={{ age, setAge, name, setData, setName, data }}>

      {children}

    </MyContext.Provider>

  );

};

  

export const useMyContext = () => {

  const context = useContext(MyContext);

  if (!context) {

    throw new Error('Use Context must be within provider');

  } else return context;

};

```
 

